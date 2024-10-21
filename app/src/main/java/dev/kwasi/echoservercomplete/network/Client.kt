package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.InetAddress
import java.net.Socket
import kotlin.concurrent.thread

class Client (private val networkMessageInterface: NetworkMessageInterface, clientId: String){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    private val PORT = 9999
    var ip:String = ""

    init {
        connectToServer("192.168.49.1", clientId)
    }
     private fun connectToServer(address: String, clientId:String) {
         try {
            clientSocket = Socket(address, PORT)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            ip = clientSocket.inetAddress.hostAddress!!

             writer.write("$clientId\n")
             writer.flush()

             val serverResponse = reader.readLine()
             if (serverResponse == "ACK") {
                 Log.d("Client", "Handshake successful connected to server")
                 listenForMessages(clientSocket)
             }else{
                 Log.e("Client", "Handshake failed. server response: $serverResponse ")
                 clientSocket.close()
             }

        }catch (e: Exception) {
            Log.e("Client", "Error Connecting to Server")
        }
    }

    private fun listenForMessages(clientSocket: Socket){
        try {
            val reader = clientSocket.getInputStream().bufferedReader()
            var message: String?
            while (clientSocket.isConnected) {
                try {
                    val serverResponse = reader.readLine()
                    if (serverResponse != null) {
                        val serverContent =
                            Gson().fromJson(serverResponse, ContentModel::class.java)
                        networkMessageInterface.onContent(serverContent)
                    }
                } catch (e: Exception) {
                    Log.e("CLIENT", "An error has occurred in the client")
                    e.printStackTrace()
                    break
                }
            }
        }catch(e: Exception) {
            Log.e("Client", "Error receiving message: ${e.message}")
        }finally {
            clientSocket.close()
        }
    }


    fun sendMessage(content: ContentModel){
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr:String = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }

    }

    fun close(){
        clientSocket.close()
    }
}