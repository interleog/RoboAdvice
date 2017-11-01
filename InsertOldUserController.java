package com.roboadvice.controller;


import com.roboadvice.model.*;
import com.roboadvice.repository.*;
import com.roboadvice.utils.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class InsertOldUserController {

    @Autowired
    StrategyRepository strategyRepository;
    @Autowired
    ApiDataRepository apiDataRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AssetsClassRepository assetsClassRepository;
    @Autowired
    AssetsRepository assetsRepository;
    @Autowired
    PortfolioRepository portfolioRepository;

    @RequestMapping(value = "/insertOldUser", method = RequestMethod.GET)
    @Transactional
    public void insertOldUser(){
        long startTime = System.currentTimeMillis();
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        String password = bcrypt.encode("ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f"); //12345678
        User u = new User(0, "Leonardo", "Galati", "leo@galati.com", password, "USER");

        if (userRepository.save(u) != null) {
            List<AssetsClass> ac = (List<AssetsClass>) assetsClassRepository.findAll();
            Strategy s1 = new Strategy(0, "ChocoStrategy", LocalDate.parse("2016-10-04"), true, new BigDecimal(15), u, ac.get(0));
            strategyRepository.save(s1);
            Strategy s2 = new Strategy(0, "ChocoStrategy", LocalDate.parse("2016-10-04"), true, new BigDecimal(50), u, ac.get(1));
            strategyRepository.save(s2);
            Strategy s3 = new Strategy(0, "ChocoStrategy", LocalDate.parse("2016-10-04"), true, new BigDecimal(25), u, ac.get(2));
            strategyRepository.save(s3);
            Strategy s4 = new Strategy(0, "ChocoStrategy", LocalDate.parse("2016-10-04"), true, new BigDecimal(10), u, ac.get(3));
            strategyRepository.save(s4);

            //======================================================================================
            //INSERT FIRST PORTFOLIO FOR THE USER
            BigDecimal investment = new BigDecimal(Constant.INITIAL_INVESTMENT);
            BigDecimal amount, value, units;
            boolean worked = false;
            ApiData api = new ApiData();
            Portfolio p = new Portfolio();
            List<Strategy> newStrategies = strategyRepository.findByUserAndDate(u, s1.getDate());

            if (newStrategies != null && !newStrategies.isEmpty()) {
                Iterable<Assets> allAssets = assetsRepository.findAll();
                for (Strategy strategy : newStrategies) {
                    for (Assets asset : allAssets) {
                        if (strategy.getAssetsClass().getId() == asset.getAssetsClass().getId()) {
                            amount = Constant.percentage(investment, strategy.getPercentage());   //(10'000 * 65)/100
                            value = Constant.percentage(amount, asset.getAllocation_p());

                            //api = apiDataRepository.findLatestValueByAssetAndDate(asset.getId(), strategy.getDate().toString());
                            api = apiDataRepository.findTopByAssetsAndDateLessThanEqualOrderByDateDesc(asset, strategy.getDate());

                            if (api != null)
                                units = value.divide(api.getValue(), 5, RoundingMode.HALF_UP);
                            else {
                                units = BigDecimal.ZERO;
                                value = BigDecimal.ZERO;
                            }

                            p = new Portfolio();
                            p.setId(0);
                            p.setUser(strategy.getUser());
                            p.setDate(strategy.getDate().plusDays(1));
                            p.setAssetsClass(strategy.getAssetsClass());
                            p.setAssets(asset);
                            p.setAmount(amount);
                            p.setValue(value);
                            p.setUnits(units);

                            portfolioRepository.save(p);
                        }
                    }
                }
                System.out.println("*** FIRST PORTFOLIO CORRECTLY ADDED ***\n");
                worked = true;
            } else {
                System.out.println("*** ERROR ADDING FIRST PORTFOLIO FOR THE USER ***\n");
                worked = false;
            }

            if (worked) {
                //===========================================================================================================
                //UPDATE USER'S PORTFOLIO EVERYDAY
                amount = BigDecimal.ZERO;
                int i = 0;

                LocalDate startDate = s1.getDate().plusDays(1);
                LocalDate endDate = LocalDate.now();
                List<Assets> assets = (List<Assets>) assetsRepository.findAll();
                List<BigDecimal> values = new ArrayList<>();
                Portfolio updatedPortfolio = new Portfolio();
                for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                    List<Portfolio> oldPortfolio = portfolioRepository.findByUserAndDate(u, date);
                    for (AssetsClass assetClass : ac) {
                        for (Assets asset : assets) {
                            if (asset.getAssetsClass().getId() == assetClass.getId()) {
                                for (Portfolio portfolio : oldPortfolio) {
                                    if (portfolio.getAssets().getId() == asset.getId()) {
                                        //api = apiDataRepository.findLatestValueByAssetAndDate(asset.getId(), date.toString());
                                        api = apiDataRepository.findTopByAssetsAndDateLessThanEqualOrderByDateDesc(asset, date);
                                        if (api != null)
                                            values.add(portfolio.getUnits().multiply(api.getValue()));
                                        else
                                            values.add(portfolio.getUnits().multiply(BigDecimal.ZERO));
                                    }
                                }
                            }
                        }

                        for (BigDecimal v : values) {
                            amount = amount.add(v);
                        }

                        for (Assets asset : assets) {
                            if (asset.getAssetsClass().getId() == assetClass.getId()) {
                                for (Portfolio portfolio : oldPortfolio) {
                                    if (portfolio.getAssets().getId() == asset.getId()) {
                                        updatedPortfolio = new Portfolio();
                                        updatedPortfolio.setId(0);
                                        updatedPortfolio.setUser(portfolio.getUser());
                                        updatedPortfolio.setDate(date.plusDays(1));
                                        updatedPortfolio.setAssetsClass(assetClass);
                                        updatedPortfolio.setAssets(asset);
                                        updatedPortfolio.setAmount(amount);
                                        updatedPortfolio.setValue(values.get(i));
                                        updatedPortfolio.setUnits(portfolio.getUnits());

                                        portfolioRepository.save(updatedPortfolio);
                                        i++;
                                    }
                                }
                            }
                        }
                        i = 0;
                        amount = BigDecimal.ZERO;
                        values.clear();
                    }
                    System.out.println("*** Portfolio correctly created for day: "+date.plusDays(1).toString()+" ***");
                }
            }

        }

        long endTime = System.currentTimeMillis();
        System.out.println("\n*** FINISHED *** - Total time: " + (endTime - startTime) / 1000 + "s\n");
    }
    //=================================================================================================================================================================
}
