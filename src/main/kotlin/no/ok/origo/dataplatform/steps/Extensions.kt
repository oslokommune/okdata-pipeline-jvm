package no.ok.origo.dataplatform.steps

import software.amazon.awssdk.services.s3.model.S3Object

fun S3Object.partitionExtension(): Pair<String, String> {
    val (filename, extension) = this.key()
        .split("/")
        .last()
        .split(".").let {
            (it.dropLast(1).joinToString(".") to it.last())
        }
    return Pair(filename, extension)
}
