package nl.tudelft.trustchain.detoks.newcoin

import java.security.PrivateKey
import java.security.PublicKey

data class OfflineFriend(val sender: PublicKey, val recipient: PublicKey) {
    public fun sign(privateKey: PrivateKey): ByteArray {
        val signature = this.encode().sign(privateKey)

        // Verify that the signature is actually correct.
        if(!this.verify(signature)) {
            throw IllegalArgumentException("Invalid private key!")
        }

        return signature
    }

    public fun verify(signature: ByteArray): Boolean {
        val encoded = this.encode()
        return encoded.verifySignature(sender, signature)
    }

    fun encode(): String {
        return "friend#${sender.encodeToString()}#${recipient.encodeToString()}"
    }
}
