package me.jameshunt.eventfarm.core

val configJson = listOf(
    """{"id":"00000000-0000-0000-0000-000000000003","className":"me.jameshunt.eventfarm.device.hs300.HS300${'$'}Config","name":"On/Off Status/set state, and power usage metrics","ip":"192.168.1.82","deviceIdPrefix": "8006D4C79A1D2CE0935A5A79B28D00291F06E0D10", "shutdownState": "0,0,0,0,0,0"}""",
    """{"id":"00000000-0000-0000-0000-000000000004","className":"me.jameshunt.eventfarm.device.hs300.HS300${'$'}Config","name":"On/Off Status/set state, and power usage metrics","ip":"192.168.1.84","deviceIdPrefix": "8006883275B48035D844595BF48CAA541F412BBC0", "shutdownState": "0,0,0,0,1,0"}""", // index 4 keep fan and sensor powered
//    """{"id":"00000000-0000-0000-0000-000000000102","className":"me.jameshunt.eventfarm.device.hs300.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at a position","ip":"192.168.1.82"}""",
    """{"id":"00000000-0000-0000-0000-000000000103","className":"me.jameshunt.eventfarm.device.AndroidCamera${'$'}Config","name":"Android Camera"}""",
    """{"id":"00000000-0000-0000-0000-000000000005","className":"me.jameshunt.eventfarm.device.ezohum.AtlasScientificEzoHum${'$'}Config","name":"temp and humidity sensor","mqttTopic":"ezoHum"}""",
//    """{"id":"00000000-0000-0000-0003-000000000000","className":"me.jameshunt.eventfarm.device.DepthSensorInput${'$'}Config","name":"returns distance water is from the sensor, and a percent remaining based on the config depth ","depthOfTankCentimeters":"32.0","depthWhenFullCentimeters":4.0}""", // 32 is distance from water still covering the pump intake
//    """{"id":"00000000-0000-0000-0003-100000000000","className":"me.jameshunt.eventfarm.device.DepthSensorInputMQTT${'$'}Config","name":"returns distance water is from the sensor, and a percent remaining based on the config depth ","depthOfTankCentimeters":"32.0","depthWhenFullCentimeters":4.0,"mqttTopic":"humidifierTankDepthSensor"}""", // 32 is distance from water still covering the pump intake
    """{"id":"00000000-0000-0000-0000-000000000007","className":"me.jameshunt.eventfarm.vpd.VPDFunction${'$'}Config","temperatureId":"00000000-0000-0000-0000-000000000005","humidityId":"00000000-0000-0000-0000-000000000005"}""",

    """{"id":"00000000-0000-0000-0001-000000000000","className":"me.jameshunt.eventfarm.vpd.VPDController${'$'}Config","vpdInputId":"00000000-0000-0000-0000-000000000007","humidifierOutputId":"00000000-0000-0000-0000-000000000003","humidifierOutputIndex": 5}""",
//    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.customcontroller.ECPHExclusiveLockController${'$'}Config","ecInputId":"00000000-0000-0000-0000-000000000005","phInputId":"00000000-0000-0000-0000-000000000006"}""",
//    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.device.ezohum.AtlasScientificEzoHumController${'$'}Config","ezoHumInputId":"00000000-0000-0000-0000-000000000005"}""",
    """{"id":"00000000-0000-0000-0004-000000000000","className":"me.jameshunt.eventfarm.customcontroller.MyLightingController${'$'}Config","lightOnOffInputId":"00000000-0000-0000-0000-000000000003", "inputIndex": 0,"lightOnOffOutputId":"00000000-0000-0000-0000-000000000003", "outputIndex": 0,"turnOnTime":"03:00","turnOffTime":"16:00"}""",
    """{"id":"00000000-0000-0000-0004-100000000000","className":"me.jameshunt.eventfarm.customcontroller.WateringController${'$'}Config","wateringOutputId":"00000000-0000-0000-0000-000000000003", "wateringOutputIndex": 1,"periodMillis":"1400000", "durationMillis": 1500, "waterDepthInputId":"00000000-0000-0000-0003-100000000000", "waterDepthIndex":null,"conserveWaterUsagePercent":0.6,"conserveWaterReservoirPercent":0.15}""",
    """{"id":"00000000-0000-0000-0004-200000000000","className":"me.jameshunt.eventfarm.customcontroller.PressurePumpController${'$'}Config","pressurePumpOnOffOutputId":"00000000-0000-0000-0000-000000000004", "pressurePumpOnOffOutputIndex": 0,"periodMillis":"12000000", "durationMillis": 140000, "waterDepthInputId":"00000000-0000-0000-0003-100000000000", "waterDepthIndex":null}""",
    """{"id":"00000000-0000-0000-0004-300000000000","className":"me.jameshunt.eventfarm.customcontroller.PeriodicController${'$'}Config","name":"Time-lapse camera","schedulableId":"00000000-0000-0000-0000-000000000103", "schedulableIndex": null,"periodMillis":"14400000", "durationMillis": null}""",
    """{"id":"00000000-0000-0000-0004-400000000000","className":"me.jameshunt.eventfarm.customcontroller.PeriodicController${'$'}Config","name":"Drain pump","schedulableId":"00000000-0000-0000-0000-000000000003", "schedulableIndex": 3,"periodMillis":"43200000", "durationMillis": 40000}""",
//    """{"id":"00000000-0000-0000-0004-500000000000","className":"me.jameshunt.eventfarm.customcontroller.PeriodicController${'$'}Config","name":"Exhaust fan","schedulableId":"00000000-0000-0000-0000-000000000003", "schedulableIndex": 4,"periodMillis":"90000", "durationMillis": 10000}""",
)
