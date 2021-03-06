package io.github.binaryfoo.decoders

import io.github.binaryfoo.*
import io.github.binaryfoo.crypto.CaPublicKeyTable
import io.github.binaryfoo.crypto.RecoveredPublicKeyCertificate
import io.github.binaryfoo.decoders.annotator.SignedDataDecoder
import io.github.binaryfoo.tlv.ISOUtil

/**
 * EMV 4.3 Book2, Table 6: Format of Data Recovered from Issuer Public Key Certificate
 */
class IssuerPublicKeyDecoder : SignedDataDecoder {

  override fun decodeSignedData(session: DecodeSession, decoded: List<DecodedData>) {
    val keyIndex = decoded.findValueForTag(EmvTags.CA_PUBLIC_KEY_INDEX)
    val encryptedCertificate = decoded.findTlvForTag(EmvTags.ISSUER_PUBLIC_KEY_CERTIFICATE)
    val rid = extractRid(decoded.findValueForTag(EmvTags.DEDICATED_FILE_NAME))
    if (keyIndex != null && encryptedCertificate != null && rid != null) {
      val caPublicKey = CaPublicKeyTable.getEntry(rid, keyIndex)
      if (caPublicKey != null) {
        for (decodedCertificate in decoded.findAllForTag(EmvTags.ISSUER_PUBLIC_KEY_CERTIFICATE)) {
          val result = recoverCertificate(encryptedCertificate, decodedCertificate, caPublicKey, ::decodeIssuerPublicKey)
          if (result.certificate != null) {
            result.certificate.rightKeyPart = decoded.findValueForTag(EmvTags.ISSUER_PUBLIC_KEY_REMAINDER)
            result.certificate.exponent = decoded.findValueForTag(EmvTags.ISSUER_PUBLIC_KEY_EXPONENT)
            session.issuerPublicKeyCertificate = result.certificate
          }
        }
      }
    }
  }

  fun extractRid(fileName: String?) = fileName?.substring(0, 10)

}

fun decodeIssuerPublicKey(recovered: ByteArray, byteLengthOfCAModulus: Int, startIndexInBytes: Int): RecoveredPublicKeyCertificate {
  val publicKeyLength = Integer.parseInt(ISOUtil.hexString(recovered, 13, 1), 16)
  val exponentLength = ISOUtil.hexString(recovered, 14, 1)
  var lengthOfLeftKeyPart = if (publicKeyLength > byteLengthOfCAModulus - 36) byteLengthOfCAModulus - 36 else publicKeyLength
  val leftKeyPart = ISOUtil.hexString(recovered, 15, lengthOfLeftKeyPart)
  val b = listOf(
      DecodedData.byteRange("Header", recovered, 0, 1, startIndexInBytes),
      DecodedData.byteRange("Format", recovered, 1, 1, startIndexInBytes),
      DecodedData.byteRange("Identifier (PAN prefix)", recovered, 2, 4, startIndexInBytes),
      DecodedData.byteRange("Expiry Date (MMYY)", recovered, 6, 2, startIndexInBytes),
      DecodedData.byteRange("Serial number", recovered, 8, 3, startIndexInBytes),
      DecodedData.byteRange("Hash algorithm", recovered, 11, 1, startIndexInBytes),
      DecodedData.byteRange("Public key algorithm", recovered, 12, 1, startIndexInBytes),
      DecodedData.byteRange("Public key length", publicKeyLength.toString(), 13, 1, startIndexInBytes),
      DecodedData.byteRange("Public key exponent length", exponentLength, 14, 1, startIndexInBytes),
      DecodedData.byteRange("Public key", leftKeyPart, 15, lengthOfLeftKeyPart, startIndexInBytes),
      DecodedData.byteRange("Hash", recovered, 15 + byteLengthOfCAModulus - 36, 20, startIndexInBytes),
      DecodedData.byteRange("Trailer", recovered, 15 + byteLengthOfCAModulus - 36 + 20, 1, startIndexInBytes)
  )
  return RecoveredPublicKeyCertificate("Issuer", b, exponentLength, leftKeyPart)
}
