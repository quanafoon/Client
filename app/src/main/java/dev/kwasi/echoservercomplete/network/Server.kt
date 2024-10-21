package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.Exception
import kotlin.concurrent.thread

/// The [Server] class has all the functionality that is responsible for the 'server' connection.
/// This is implemented using TCP. This Server class is intended to be run on the GO.

class Server(private val iFaceImpl:NetworkMessageInterface) {
    companion object {
        const val PORT: Int = 9999

    }

    private val svrSocket: ServerSocket = ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))
    private val clientMap: HashMap<String, Socket> = HashMap()
    private var isRunning = false
    init {
        isRunning = true
        thread{
            while(isRunning){
                try{
                    val clientConnectionSocket = svrSocket.accept()
                    Log.e("SERVER", "The server has accepted a connection: ")
                    handleSocket(clientConnectionSocket)

                }catch (e: Exception){
                    Log.e("SERVER", "An error has occurred in the server!")
                    e.printStackTrace()
                }
            }
        }
    }


    private fun handleSocket(socket: Socket){
        socket.inetAddress.hostAddress?.let {
            clientMap["Bob"] = socket
            Log.e("SERVER", "A new connection has been detected!")
            thread {

                while(socket.isConnected){
                    try{
                        listenForMessages(socket)
                    } catch (e: Exception){
                        Log.e("SERVER", "An error has occurred with the client $it")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun sendMessage(content: ContentModel){
        thread{
            val writer = clientMap["Bob"]?.getOutputStream()?.bufferedWriter()
            val contentAsStr:String = Gson().toJson(content)
            writer?.write("$contentAsStr\n")
            writer?.flush()
        }
    }

    private fun listenForMessages(clientSocket: Socket) {
        val reader = clientSocket.inputStream.bufferedReader()
            try {
                while (clientSocket.isConnected) {
                    val receivedMessage = reader.readLine()
                    if (receivedMessage != null) {
                        Log.e("Client", "Received: $receivedMessage")
                        val message =Gson().fromJson(receivedMessage, ContentModel::class.java)
                        iFaceImpl.onContent(message)
                    }
                }
            } catch (e: Exception) {
                Log.e("Client", "Error receiving message: ${e.message}")
            }
    }



    fun close(){
        isRunning = false
        svrSocket.close()
        clientMap.clear()
    }

}