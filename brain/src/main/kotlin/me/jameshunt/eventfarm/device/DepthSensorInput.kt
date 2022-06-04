package me.jameshunt.eventfarm.device

import gnu.io.CommPortIdentifier
import gnu.io.SerialPort
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.jameshunt.eventfarm.core.Configurable
import me.jameshunt.eventfarm.core.Input
import me.jameshunt.eventfarm.core.Logger
import me.jameshunt.eventfarm.core.TypedValue
import java.io.Closeable
import java.io.InputStream
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

@Deprecated("use mqtt version instead")
class DepthSensorInput(override val config: Config, private val logger: Logger) : Input, Closeable {
    data class Config(
        override val id: UUID,
        override val className: String,
        val depthOfTankCentimeters: Float,
        val depthWhenFullCentimeters: Float
    ) : Configurable.Config

    // linux sometimes doesn't recognize /dev/ttyAMCxx ports
    // may need to symlink to a different one
    // sudo ln -s /dev/ttyACM0 /dev/ttyS33
    // TODO: include this in startup script?
    // https://stackoverflow.com/questions/33480769/commportidentifier-getportidentifiers-rxtx-not-listing-all-ports#comment55117950_33649749
    private val portName = "/dev/ttyS33" // todo: move to config

    /** communication with serial port using blocking call */
    private val serialCommunicationExecutor = Executors.newSingleThreadExecutor()

    private val waterDistance = PublishSubject.create<TypedValue.Length.Centimeter>()
    private val error = PublishSubject.create<TypedValue.Error>()

    init {
        listenRetryForever()
    }

    override fun getInputEvents(): Observable<Input.InputEvent> {
        val resultEvents = waterDistance.flatMap { cm ->
            val time = Instant.now()
            val depthCorrected = cm.value - config.depthWhenFullCentimeters
            val depthOfTankCorrected = config.depthOfTankCentimeters - config.depthWhenFullCentimeters
            val missing = depthCorrected / depthOfTankCorrected
            val percentRemaining = (1f - missing).coerceIn(0f, 1f)
            Observable.just(
                Input.InputEvent(
                    inputId = config.id,
                    index = null,
                    time = time,
                    value = cm
                ),
                Input.InputEvent(
                    inputId = config.id,
                    index = null,
                    time = time,
                    value = TypedValue.Percent(percentRemaining)
                )
            )
        }

        val errorEvents = error.map { error ->
            Input.InputEvent(config.id, null, Instant.now(), error)
        }

        return Observable.merge(resultEvents, errorEvents)
    }

    override fun close() {
        serialCommunicationExecutor.shutdownNow()
        waterDistance.onComplete()
        error.onComplete()
    }

    private fun listenRetryForever() {
        serialCommunicationExecutor.execute {
            try {
                connect().listen()
            } catch (e: Exception) {
                logger.error("could not read depth sensor serial port data", e)
                error.onNext(TypedValue.Error(e))
                Thread.sleep(30_000)
                listenRetryForever()
            }
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
        val scanner = Scanner(this)
        // hasNextLine is a blocking call
        while (scanner.hasNextLine()) {
            val distanceInCentimeters = scanner.nextLine().toFloat()
            waterDistance.onNext(TypedValue.Length.Centimeter(distanceInCentimeters))
        }
        scanner.close()
        close()
    }
}