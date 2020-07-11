package monix.connect.gcs.components

import java.nio.ByteBuffer

import com.google.cloud.WriteChannel
import com.google.cloud.storage.{BlobInfo, Storage}
import com.google.cloud.storage.Storage.BlobWriteOption
import monix.connect.gcs.GcsStorage
import monix.eval.Task
import monix.execution.cancelables.AssignableCancelable
import monix.execution.{Ack, Callback, Scheduler}
import monix.reactive.Consumer
import monix.reactive.observers.Subscriber

import scala.concurrent.Future
import scala.util.control.NonFatal

private[gcs] final class GcsUploader(storage: Storage, blobInfo: BlobInfo, chunkSize: Int, options: BlobWriteOption*) extends Consumer[Array[Byte], Unit] {
  override def createSubscriber(cb: Callback[Throwable, Unit], s: Scheduler): (Subscriber[Array[Byte]], AssignableCancelable) = {
    val sub = new Subscriber[Array[Byte]] {
      self =>
      override implicit def scheduler: Scheduler = s

      val writer: WriteChannel = storage.writer(blobInfo, options: _*)
      writer.setChunkSize(chunkSize)

      override def onNext(chunk: Array[Byte]): Future[Ack] = {
        Task {
          try {
            if (chunk.isEmpty) {
              onComplete()
              Ack.Stop
            }
            else {
              writer.write(ByteBuffer.wrap(chunk))
              monix.execution.Ack.Continue
            }

          } catch {
            case ex if NonFatal(ex) => {
              onError(ex)
              Ack.Stop
            }
          }
        }.runToFuture
      }

      override def onError(ex: Throwable): Unit = {
        writer.close()
        cb.onError(ex)
      }

      override def onComplete(): Unit =
        writer.close()
        cb.onSuccess(())
    }

    (sub, AssignableCancelable.dummy)
  }
}

object GcsUploader {

  def apply(storage: Storage, blobInfo: BlobInfo, chunkSize: Int = 4096, options: Seq[BlobWriteOption] = Seq.empty): GcsUploader = new GcsUploader(storage, blobInfo, chunkSize, options:_ *)

  def apply(storage: GcsStorage, blobInfo: BlobInfo, chunkSize: Int = 4096, options: Seq[BlobWriteOption] = Seq.empty): GcsUploader = new GcsUploader(storage.underlying, blobInfo, chunkSize, options:_ *)
}

