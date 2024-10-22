package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import kotlin.concurrent.thread
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.SecretKey
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Client(private val networkMessageInterface: NetworkMessageInterface, var studentId: String){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    private val PORT = 9999
    var ip:String = ""
    private val studentIdHash = hashStudentId(studentId)
    private val aesKey = generateAESKey(studentIdHash)
    private val aesIv = generateIV(studentIdHash)

    init {
        thread {
            clientSocket = Socket("192.168.49.1", PORT)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            ip = clientSocket.inetAddress.hostAddress!!
            val handshakeContent = ContentModel(studentId, ip)
            val handshakeAsStr = Gson().toJson(handshakeContent)
            writer.write("$handshakeAsStr\n")
            writer.flush()

            val studentIdHash = hashStudentId(studentId)
            val aesKey = generateAESKey(studentIdHash)
            val aesIv = generateIV(studentIdHash)
            val serverChallenge = reader.readLine()
            val contentR = Gson().fromJson(serverChallenge, ContentModel::class.java)
            val R = contentR.message
            val encryptedText = encryptMessage(R, aesKey, aesIv)
            val encryptedContent = ContentModel(encryptedText, ip)
            val encryptedContentStr = Gson().toJson(encryptedContent)
            writer.write("$encryptedContentStr\n")
            writer.flush()

            while(true){
                try{
                    val serverResponse = reader.readLine()
                    if (serverResponse != null){
                        val serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)
                        val decryptedMessage = decryptMessage(serverContent.message, aesKey, aesIv)
                        val onScreenMessage = ContentModel(decryptedMessage, serverContent.senderIp)
                        networkMessageInterface.onContent(onScreenMessage)
                    }
                } catch(e: Exception){
                    Log.e("CLIENT", "An error has occurred in the client")
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    fun sendMessage(content: ContentModel){
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val encryptedText = encryptMessage(content.message, aesKey, aesIv)
            val encryptedContent = ContentModel(encryptedText, content.senderIp)
            val contentAsStr:String = Gson().toJson(encryptedContent)
            writer.write("$contentAsStr\n")
            writer.flush()
        }

    }

    fun getFirstNChars(str: String, n:Int) = str.substring(0,n)
    fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    fun hashStudentId(studentID : String) : String{
        val algorithm = "SHA-256"
        val hashedString = MessageDigest.getInstance(algorithm).digest(studentID.toByteArray(UTF_8))
        return hashedString.toHex();
    }

    fun generateAESKey(seed: String): SecretKeySpec {
        val first32Chars = getFirstNChars(seed, 32)
        val aesKey = SecretKeySpec(first32Chars.toByteArray(), "AES")
        return aesKey
    }

    fun generateIV(seed: String): IvParameterSpec {
        val first16Chars = getFirstNChars(seed, 16)
        return IvParameterSpec(first16Chars.toByteArray())
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encryptMessage(plaintext: String, aesKey:SecretKey, aesIv: IvParameterSpec):String{
        val plainTextByteArr = plaintext.toByteArray()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv)

        val encrypt = cipher.doFinal(plainTextByteArr)
        return Base64.Default.encode(encrypt)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decryptMessage(encryptedText: String, aesKey:SecretKey, aesIv: IvParameterSpec):String{
        val textToDecrypt = Base64.Default.decode(encryptedText)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

        cipher.init(Cipher.DECRYPT_MODE, aesKey,aesIv)

        val decrypt = cipher.doFinal(textToDecrypt)
        return String(decrypt)

    }

    fun close(){
        clientSocket.close()
    }
}