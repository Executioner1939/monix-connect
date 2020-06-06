package monix.connect.gcs.components

import java.nio.channels.Channels

import com.google.cloud.ReadChannel
import com.google.cloud.storage.{BlobId, Storage}
import monix.eval.Task
import monix.reactive.Observable

trait StorageDownloader {

  private def openReadChannel(storage: Storage, bucket: String, blobId: BlobId, chunkSize: Int): Observable[ReadChannel] = {
    Observable.resource {
      Task {
        val reader = storage.reader(bucket, blobId.getName)
        reader.setChunkSize(chunkSize)
        reader
      }
    } { reader =>
      Task(reader.close())
    }
  }

  protected def download(storage: Storage, bucket: String, blobId: BlobId, chunkSize: Int): Observable[Array[Byte]] = {
    openReadChannel(storage, bucket, blobId, chunkSize).flatMap { channel =>
      Observable.fromInputStreamUnsafe(Channels.newInputStream(channel), chunkSize)
    }.takeWhile(_.nonEmpty)
  }
}