package monix.connect.gcs

import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper
import com.google.cloud.storage.{Blob, BlobId, BlobInfo, Option => _}
import monix.execution.Scheduler.Implicits.global
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class GcsStorageSuite extends AnyWordSpecLike with IdiomaticMockito with Matchers with ArgumentMatchersSugar with BeforeAndAfterAll {

  val storage = LocalStorageHelper.getOptions.getService
  val nonEmptyString: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(chars => "test-" + chars.mkString.take(20))
  val testBucketName = nonEmptyString.sample.get


  s"${GcsStorage}" should {

    "get existing blob from its id " in {
      //given
      val blobName = nonEmptyString.sample.get
      val blobInfo: BlobInfo = BlobInfo.newBuilder(BlobId.of(testBucketName, blobName)).build
      val content: Array[Byte] = nonEmptyString.sample.get.getBytes()
      storage.create(blobInfo, content)
      val gcsStorage = GcsStorage(storage)

      //when
      val t = gcsStorage.getBlob(testBucketName, blobName)
      val gcsBlob: Option[GcsBlob] = t.runSyncUnsafe()

      //then
      gcsBlob.isDefined shouldBe true
    }

    "return empty when getting non existing blob from its id" in {
      //given
      val blobName = nonEmptyString.sample.get
      val gcsStorage = GcsStorage(storage)

      //when
      val t = gcsStorage.getBlob(testBucketName, blobName)
      val gcsBlob: Option[GcsBlob] = t.runSyncUnsafe()

      //then
      gcsBlob.isDefined shouldBe false
    }

    "get exhaustively the list of existing blobs from the the given blob ids" in {
      //given two not and existing blobs
      val blob1 = BlobInfo.newBuilder(BlobId.of(testBucketName, nonEmptyString.sample.get)).build
      val blob2 = BlobInfo.newBuilder(BlobId.of(testBucketName, nonEmptyString.sample.get)).build
      val nonExistingBlob = BlobInfo.newBuilder(BlobId.of(testBucketName, nonEmptyString.sample.get)).build
      val content: Array[Byte] = nonEmptyString.sample.get.getBytes()
      storage.create(blob1, content)
      storage.create(blob2, content)
      val gcsStorage = GcsStorage(storage)

      //when
      val t = gcsStorage.getBlobs(List(blob1.getBlobId, blob2.getBlobId, nonExistingBlob.getBlobId))
      val gcsBlob: List[GcsBlob] = t.runSyncUnsafe()

      //then
      gcsBlob.size shouldBe 2
    }

  }

}
