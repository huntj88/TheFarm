package me.jameshunt.eventfarm.device

import gnu.io.CommPortIdentifier
import gnu.io.SerialPort
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Input
import me.jameshunt.eventfarm.core.TypedValue
import java.io.InputStream
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors


class DepthSensorInput(override val config: Config) : Input {
    data class Config(
        override val id: UUID,
        override val className: String,
        val depthOfTankCentimeters: Int,
        val depthWhenFullCentimeters: Int
    ) : Configurable.Config

    // linux sometimes doesn't recognize /dev/ttyAMCxx ports
    // may need to symlink to a different one
    // sudo ln -s /dev/ttyACM0 /dev/ttyS33
    // TODO: include this in startup script?
    // https://stackoverflow.com/questions/33480769/commportidentifier-getportidentifiers-rxtx-not-listing-all-ports#comment55117950_33649749
    private val portName = "/dev/ttyS33"

    private val distanceFromWaterCentimeters = PublishSubject.create<Int>()

    init {
        connect().listen()
    }

    override fun getInputEvents(): Observable<Input.InputEvent> {
        return distanceFromWaterCentimeters.flatMap { cm ->
            val time = Instant.now()
            val depthCmF = cm.toFloat()
            val depthCorrected = depthCmF - config.depthWhenFullCentimeters
            val depthOfTankCorrected = config.depthOfTankCentimeters - config.depthWhenFullCentimeters
            val missing = depthCorrected / depthOfTankCorrected
            val percentRemaining = (1f - missing).coerceIn(0f, 1f)
            Observable.just(
                Input.InputEvent(
                    inputId = config.id,
                    index = null,
                    time = time,
                    value = TypedValue.Length.Centimeter(depthCmF)
                ),
                Input.InputEvent(
                    inputId = config.id,
                    index = null,
                    time = time,
                    value = TypedValue.Percent(percentRemaining)
                )
            )
        }
    }

    private fun connect(): InputStream {
        val portIdentifier = CommPortIdentifier.getPortIdentifier(portName)
        if (portIdentifier.isCurrentlyOwned) {
            throw IllegalStateException("Error: Port is currently in use")
        }
        val commPort = (portIdentifier.open(this.javaClass.name, 2000) as? SerialPort)
            ?: throw IllegalStateException("Error: Only serial ports are handled")

        commPort.setSerialPortParams(
            9600,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE
        )
        return commPort.inputStream
    }

    private fun InputStream.listen() {
        // TODO: subscribe and check for completion, should never complete
        Executors.newSingleThreadExecutor().execute {
            val scanner = Scanner(this)
            // hasNextLine is a blocking call
            while (scanner.hasNextLine()) {
                val distanceInCentimeters = scanner.nextLine().toInt()
                distanceFromWaterCentimeters.onNext(distanceInCentimeters)
            }
            scanner.close()
        }
    }
}