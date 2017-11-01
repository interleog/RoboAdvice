RoboAdviceApp.controller("demoController", function($scope, $log, demoREST, demoService, userService, strategyService){

    $log.debug("demo controller active");
    $scope.standardStrategies = strategyService.getStandardStrategies();
    $scope.user = userService;
    $scope.strategy = strategyService;
    $scope.forecastDatas = {};

    /* bindings for the graph component */
    $scope.amount = null;
    $scope.date = null;
    $scope.timestamp = null;

    $scope.buttonClicked = true;
    $scope.spinner = false;

    if(userService.hasStrategies()){
      $scope.isCustom = true;
      /* check if the current strategy is custom or not */
      let currentPercentage = strategyService.getCurrentStrategy().getAssets();
      $scope.standardStrategies.forEach(function(strategy){
        // for each strategy get the percentages
        let standardPercentage = strategy.strategy.map(function(st){
          return st.percentage;
        });

        if(
          standardPercentage[0] == currentPercentage[0] &&
          standardPercentage[1] == currentPercentage[1] &&
          standardPercentage[2] == currentPercentage[2] &&
          standardPercentage[3] == currentPercentage[3]
        ){
          $log.debug("the current strategy is a standard strategy");
          $scope.isCustom = false;
        }
      });
    }
    demoService.getDemoForecasting(function(response){
      if(response.statusCode == 0) {
        //$log.debug(response.data);
        $scope.forecastDatas = response.data;
      }else{
        $log.debug("demoController | statusCode = 1");
      }
    });

    /*
      BacktestingCallback is called after the demo backtesting request to the server
    */
    $scope.backtestingCallback = function(response){
      if(response.statusCode == 0){
        $log.debug("backtesting calling ok");
        $scope.amount = new Array(response.data.length);
        $scope.timestamp = new Array(response.data.length);
        $scope.date = new Array(response.data.length);

        $scope.spinner = false;
        for (let i = 0; i < response.data.length; i++) {
          $scope.amount[i] = response.data[i].totalAmount;
          $scope.date[i] = response.data[i].date.year + '/' + response.data[i].date.monthValue + '/' + response.data[i].date.dayOfMonth;
          $scope.timestamp[i] = new Date($scope.date[i]).getTime();
        }
        //$log.debug("backtesting response from the server:")
        //$log.debug($scope.amount);
        //$log.debug($scope.timestamp);

      }else{
        $log.error("backtesting calling error")
      }
    }

    $scope.update = function(param) {
        $scope.buttonClicked = false;
        let formattedDate;
        let date = new Date($scope.interval);

        formattedDate = date.getFullYear() + "-" + (date.getMonthFormatted()) + "-" + date.getDayFormatted();
        $log.debug(formattedDate);
        $scope.spinner = true;

        switch(param){
          case "Bonds":
            $log.debug("backtesting on bonds strategy");
            demoService.backtesting.bonds(formattedDate).$promise.then($scope.backtestingCallback);
          break;
          case "Income":
            $log.debug("backtesting on income strategy");
            demoService.backtesting.income(formattedDate).$promise.then($scope.backtestingCallback);
          break;
          case "Balanced":
            $log.debug("backtesting on balanced strategy");
            demoService.backtesting.balanced(formattedDate).$promise.then($scope.backtestingCallback);
          break;
          case "Growth":
            $log.debug("backtesting on growth strategy");
            demoService.backtesting.growth(formattedDate).$promise.then($scope.backtestingCallback);
          break;
          case "Stocks":
            $log.debug("backtesting on stock strategy");
            demoService.backtesting.stocks(formattedDate).$promise.then($scope.backtestingCallback);
          break;
          default:
            demoService.backtesting.bonds(formattedDate).$promise.then($scope.backtestingCallback);
            //
            // let strategy = strategyService.getCurrentStrategy();
            // $log.debug("backtesting on current strategy");
            // $log.debug("fromdate: " + formattedDate);
            // $log.debug("name:" + strategy.getName());
            // $log.debug("percentage: " + strategy.getAssets());
            // demoService.backtesting.custom(strategy.getName(),strategy.getAssets(),formattedDate);
        }

    }//end update


    /*
      UpdateOwn provide backtesting for the current user's strategy
    */
    $scope.updateOwn = function(param1, param2){
      let formattedDate;
      let date = new Date($scope.interval);

      formattedDate = date.getFullYear() + "-" + (date.getMonthFormatted()) + "-" + date.getDayFormatted();
      $log.debug(formattedDate);
      $log.debug("backtesting on your strategy",param1,param2);
      demoService.backtesting.custom(param1,param2, formattedDate).$promise.then(function (response) {

        $scope.amount = new Array(response.data.length);
        $scope.timestamp = new Array(response.data.length);
        $scope.date = new Array(response.data.length);

        for (let i = 0; i < response.data.length; i++) {
          $scope.amount[i] = response.data[i].totalAmount;
          $scope.date[i] = response.data[i].date.year + '/' + response.data[i].date.monthValue + '/' + response.data[i].date.dayOfMonth;
          $scope.timestamp[i] = new Date($scope.date[i]).getTime();
        }
        $log.debug($scope.amount);
        $log.debug($scope.timestamp);
      });
    }


});
