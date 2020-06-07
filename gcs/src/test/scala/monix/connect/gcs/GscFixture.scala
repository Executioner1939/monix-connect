package monix.connect.gcs

import com.google.cloud.storage.{Acl, BlobId, BlobInfo, Cors, StorageClass}
import com.google.cloud.storage.Acl.{Entity, Group, Project, Role, User}
import com.google.cloud.storage.BucketInfo.LifecycleRule.{DeleteLifecycleAction, LifecycleAction, LifecycleCondition}
import com.google.cloud.storage.BucketInfo.{IamConfiguration, LifecycleRule, Logging}
import monix.connect.gcs.configuration.{GcsBlobInfo, GcsBucketInfo}
import org.scalacheck.Gen

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

trait GscFixture {

  val genBool = Gen.oneOf(true, false)

  val genAcl: Gen[Acl] = for {
    entity <- Gen.oneOf[Entity](User.ofAllUsers(), new Group("sample@email.com"), new Project(Project.ProjectRole.OWNERS, "id"))
    role <- Gen.oneOf(Role.OWNER, Role.READER, Role.WRITER)
  } yield {
    Acl.of(entity , role)
  }

  val genStorageClass: Gen[StorageClass] = Gen.oneOf(StorageClass.ARCHIVE, StorageClass.COLDLINE, StorageClass.DURABLE_REDUCED_AVAILABILITY, StorageClass.MULTI_REGIONAL, StorageClass.NEARLINE, StorageClass.REGIONAL, StorageClass.STANDARD)
  val genHex: Gen[String] = Gen.oneOf("1100", "12AC")

  val genGscBlobInfo: Gen[BlobInfo] = for {
    bucket <- Gen.alphaLowerStr
    name <- Gen.alphaLowerStr
    contentType <- Gen.option(Gen.alphaLowerStr)
    contentDisposition <- Gen.option(Gen.alphaLowerStr)
    contentLanguage <- Gen.option(Gen.alphaLowerStr)
    contentEncoding <- Gen.option(Gen.alphaLowerStr)
    cacheControl <- Gen.option(Gen.alphaLowerStr)
    crc32c <- Gen.option(Gen.alphaLowerStr.map(_.hashCode.toString))
    crc32cFromHexString <- Gen.option(genHex)
    md5 <- Gen.option(Gen.alphaLowerStr.map(_.hashCode.toString))
    md5FromHexString <- Gen.option(genHex)
    storageClass <- Gen.option(genStorageClass)
    temporaryHold <- Gen.option(Gen.oneOf(true, false))
    eventBasedHold <- Gen.option(Gen.oneOf(true, false))
    acl <- Gen.listOf(genAcl)
    metadata <- Gen.mapOfN(3, ("k", "v"))
  } yield {
    val builder = BlobInfo.newBuilder(BlobId.of(bucket, name))
    contentType.foreach(builder.setContentType)
      contentDisposition.foreach(builder.setContentDisposition)
      contentLanguage.foreach(builder.setContentLanguage)
      contentEncoding.foreach(builder.setContentEncoding)
      cacheControl.foreach(builder.setCacheControl)
      crc32c.foreach(builder.setCrc32c)
      crc32cFromHexString.foreach(builder.setCrc32cFromHexString)
      md5.foreach(builder.setMd5)
      md5FromHexString.foreach(builder.setMd5FromHexString)
      storageClass.foreach(builder.setStorageClass(_))
      temporaryHold.foreach(builder.setEventBasedHold(_))
      eventBasedHold.foreach(b => builder.setEventBasedHold(b))
      builder.setAcl(acl.asJava)
      builder.setMetadata(metadata.asJava)
      builder.build()
  }

  val genBlobInfoMetadata = for {
    contentType <- Gen.option(Gen.alphaLowerStr)
    contentDisposition <- Gen.option(Gen.alphaLowerStr)
    contentLanguage <- Gen.option(Gen.alphaLowerStr)
    contentEncoding <- Gen.option(Gen.alphaLowerStr)
    cacheControl <- Gen.option(Gen.alphaLowerStr)
    crc32c <- Gen.option(Gen.alphaLowerStr) //
    crc32cFromHexString <- Gen.option(genHex)
    md5 <- Gen.option(Gen.alphaLowerStr)
    md5FromHexString <- Gen.option(genHex)
    storageClass <- Gen.option(genStorageClass)
    temporaryHold <- Gen.option(Gen.oneOf(true, false))
    eventBasedHold <- Gen.option(Gen.oneOf(true, false))
   } yield {
    GcsBlobInfo.Metadata(
      contentType = contentType,
      contentDisposition = contentDisposition,
      contentLanguage = contentLanguage,
      contentEncoding = contentEncoding,
      cacheControl = cacheControl,
      crc32c = crc32c,
      crc32cFromHexString = crc32cFromHexString,
      md5 = md5,
      md5FromHexString = md5FromHexString,
      storageClass = storageClass,
      temporaryHold = temporaryHold,
      eventBasedHold = eventBasedHold,
    )
  }

  val genIamConf = IamConfiguration.newBuilder().build()
  val genDeleteLifeCycleRule = new LifecycleRule(LifecycleAction.newDeleteAction(), LifecycleCondition.newBuilder().setIsLive(true).build())
  val genLifeCycleRules = Gen.nonEmptyListOf(genDeleteLifeCycleRule)
  val genCors = Gen.nonEmptyListOf(Cors.newBuilder().build())
  val genBucketInfoMetadata = for {
    storageClass <- Gen.option(genStorageClass)
    logging <- Gen.option(Logging.newBuilder().setLogBucket("WARN").build())
    retentionPeriod <- Gen.option(Gen.choose(1, 1000).map(_.seconds))
    versioningEnabled <- Gen.option(genBool)
    requesterPays <- Gen.option(genBool)
    eventBasedHold <- Gen.option(genBool)
    acl <- Gen.listOf(genAcl)
    defaultAcl <- Gen.listOf(genAcl)
    cors <- genCors
    lifecycleRules <- genLifeCycleRules
    iamConfiguration <- Gen.option(genIamConf)
    defaultKmsKeyName <- Gen.option(Gen.alphaLowerStr)
    labels <- Gen.mapOfN(3, ("labelKey", "labelValue"))
    indexPage <- Gen.option(Gen.alphaLowerStr)
    notFoundPage <- Gen.option(Gen.alphaLowerStr)
  } yield {
    GcsBucketInfo.Metadata(
      storageClass = storageClass,
      logging = logging,
      retentionPeriod = retentionPeriod,
      versioningEnabled = versioningEnabled,
      requesterPays = requesterPays,
      defaultEventBasedHold = eventBasedHold,
      acl = acl,
      defaultAcl = defaultAcl,
      cors = cors,
      lifecycleRules = lifecycleRules,
      iamConfiguration = iamConfiguration,
      defaultKmsKeyName = defaultKmsKeyName,
      labels = labels,
      indexPage =  indexPage,
      notFoundPage = notFoundPage
    )
  }
}
