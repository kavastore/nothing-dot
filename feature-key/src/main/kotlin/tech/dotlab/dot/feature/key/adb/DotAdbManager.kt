package tech.dotlab.dot.feature.key.adb

import android.content.Context
import android.os.Build
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateExtensions
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.KeyIdentifier
import android.sun.security.x509.PrivateKeyUsageExtension
import android.sun.security.x509.SubjectKeyIdentifierExtension
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random

/**
 * Concrete [AbsAdbConnectionManager] for Dot.: a stable RSA keypair + self-signed certificate is
 * generated once and persisted, so the phone remembers our authorization across sessions.
 */
class DotAdbManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val privateKey: PrivateKey
    private val certificate: Certificate

    init {
        setApi(Build.VERSION.SDK_INT)
        val keyFile = File(context.filesDir, "dot_adb_key")
        val certFile = File(context.filesDir, "dot_adb_cert")
        if (keyFile.exists() && certFile.exists()) {
            privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
            certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(certFile.readBytes()))
        } else {
            val keyPair = generateKeyPair()
            privateKey = keyPair.private
            certificate = generateCertificate(keyPair)
            keyFile.writeBytes(privateKey.encoded)
            certFile.writeBytes(certificate.encoded)
        }
    }

    override fun getPrivateKey(): PrivateKey = privateKey

    override fun getCertificate(): Certificate = certificate

    override fun getDeviceName(): String = "Dot."

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
        return generator.generateKeyPair()
    }

    private fun generateCertificate(keyPair: KeyPair): Certificate {
        val publicKey: PublicKey = keyPair.public
        val algorithm = "SHA512withRSA"
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + 86_400_000L * 365)
        val x500Name = X500Name("CN=Dot.")

        val extensions = CertificateExtensions()
        extensions.set(
            "SubjectKeyIdentifier",
            SubjectKeyIdentifierExtension(KeyIdentifier(publicKey).identifier),
        )
        extensions.set("PrivateKeyUsage", PrivateKeyUsageExtension(notBefore, notAfter))

        val info = X509CertInfo()
        info.set("version", CertificateVersion(2))
        info.set("serialNumber", CertificateSerialNumber(Random().nextInt() and Int.MAX_VALUE))
        info.set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algorithm)))
        info.set("subject", CertificateSubjectName(x500Name))
        info.set("key", CertificateX509Key(publicKey))
        info.set("validity", CertificateValidity(notBefore, notAfter))
        info.set("issuer", CertificateIssuerName(x500Name))
        info.set("extensions", extensions)

        return X509CertImpl(info).apply { sign(keyPair.private, algorithm) }
    }

    companion object {
        @Volatile
        private var instance: DotAdbManager? = null

        fun getInstance(context: Context): DotAdbManager =
            instance ?: synchronized(this) {
                instance ?: DotAdbManager(context.applicationContext).also { instance = it }
            }
    }
}
