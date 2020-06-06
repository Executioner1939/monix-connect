package monix.connect.gcs.components

import cats.effect.Resource
import com.google.cloud.WriteChannel
import com.google.cloud.storage.Storage.BlobWriteOption
import com.google.cloud.storage.{BlobInfo, Storage}
import monix.eval.Task

trait StorageUploader {

  private def openWriteChannel(storage: Storage, blobInfo: BlobInfo, chunkSize: Int, options: BlobWriteOption*): Resource[Task, WriteChannel] = {
    Resource.make {
      Task {
        val writer = storage.writer(blobInfo, options: _*)
        writer.setChunkSize(chunkSize)
        writer
      }
    } { writer =>
      Task(writer.close())
    }
  }

  protected def upload(storage: Storage, blobInfo: BlobInfo, chunkSize: Int, options: BlobWriteOption*): Task[StorageConsumer] = {
    openWriteChannel(storage, blobInfo, chunkSize, options: _*).use { channel =>
      Task(StorageConsumer(channel))
    }
  }
}