package me.jameshunt.eventfarm.core

val configJson = listOf(
    """{"id":"00000000-0000-0000-0000-000000000003","className":"me.jameshunt.eventfarm.device.hs300.HS300${'$'}Inputs${'$'}Config","name":"On/Off Status, and power usage metrics","ip":"192.168.1.82"}""",
    """{"id":"00000000-0000-0000-0000-000000000102","className":"me.jameshunt.eventfarm.device.hs300.HS300${'$'}OnOffOutput${'$'}Config","name":"turn plug on or off at a position","ip":"192.168.1.82"}""",
    """{"id":"00000000-0000-0000-0000-000000000005","className":"me.jameshunt.eventfarm.device.ezohum.AtlasScientificEzoHum${'$'}Config","name":"temp and humidity sensor"}""",
    """{"id":"00000000-0000-0000-0003-000000000000","className":"me.jameshunt.eventfarm.device.DepthSensorInput${'$'}Config","name":"returns distance water is from the sensor, and a percent remaining based on the config depth ","depthOfTankCentimeters":"20","depthWhenFullCentimeters":3}""",
    """{"id":"00000000-0000-0000-0000-000000000007","className":"me.jameshunt.eventfarm.vpd.VPDFunction${'$'}Config","temperatureId":"00000000-0000-0000-0000-000000000005","humidityId":"00000000-0000-0000-0000-000000000005"}""",

    """{"id":"00000000-0000-0000-0001-000000000000","className":"me.jameshunt.eventfarm.vpd.VPDController${'$'}Config","vpdInputId":"00000000-0000-0000-0000-000000000007","humidifierOutputId":"00000000-0000-0000-0000-000000000152","humidifierOutputIndex": 5}""",
//    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.customcontroller.ECPHExclusiveLockController${'$'}Config","ecInputId":"00000000-0000-0000-0000-000000000005","phInputId":"00000000-0000-0000-0000-000000000006"}""",
    """{"id":"00000000-0000-0000-0002-000000000000","className":"me.jameshunt.eventfarm.device.ezohum.AtlasScientificEzoHumController${'$'}Config","ezoHumInputId":"00000000-0000-0000-0000-000000000005"}""",
    """{"id":"00000000-0000-0000-0004-000000000000","className":"me.jameshunt.eventfarm.customcontroller.MyLightingController${'$'}Config","lightOnOffInputId":"00000000-0000-0000-0000-000000000003", "inputIndex": 0,"lightOnOffOutputId":"00000000-0000-0000-0000-000000000102", "outputIndex": 0,"turnOnTime":"03:00","turnOffTime":"16:00"}"""
)