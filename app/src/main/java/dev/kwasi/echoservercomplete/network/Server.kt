package dev.kwasi.echoservercomplete.network

import android.util.Log
import com.google.gson.Gson
import dev.kwasi.echoservercomplete.models.ContentModel
import java.io.BufferedReader
import java.io.BufferedWriter
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
        private val clientMap: HashMap<String, Socket> = HashMap()


        fun getClientMap() : HashMap<String, Socket> {
            return clientMap
        }

        fun updateClientMap(student: String, socket: Socket) {
            clientMap[student] = socket


        }

    }



    private val svrSocket: ServerSocket =
        ServerSocket(PORT, 0, InetAddress.getByName("192.168.49.1"))


    init {
        thread{
            while(true){
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
            clientMap[it] = socket

            Log.e("SERVER", "A new connection has been detected!")
            thread {
                val clientReader = socket.inputStream.bufferedReader()
                val clientWriter = socket.outputStream.bufferedWriter()
                var receivedJson: String?

               // handshake(socket, clientReader, clientWriter)
                while(socket.isConnected){
                    try{
                        receivedJson = clientReader.readLine()
                        if (receivedJson!= null){
                            Log.e("SERVER", "Received a message from client $it")
                            val clientContent = Gson().fromJson(receivedJson, ContentModel::class.java)
                            val reversedContent = ContentModel(clientContent.message.reversed(), "192.168.49.1")

                            val reversedContentStr = Gson().toJson(reversedContent)
                            clientWriter.write("$reversedContentStr\n")
                            clientWriter.flush()

                            // To show the correct alignment of the items (on the server), I'd swap the IP that it came from the client
                            // This is some OP hax that gets the job done but is not the best way of getting it done.
                            val tmpIp = clientContent.senderIp
                            clientContent.senderIp = reversedContent.senderIp
                            reversedContent.senderIp = tmpIp

                            iFaceImpl.onContent(clientContent)
                            iFaceImpl.onContent(reversedContent)

                        }
                    } catch (e: Exception){
                        Log.e("SERVER", "An error has occurred with the client $it")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun close(){
        svrSocket.close()
        clientMap.clear()
    }

 /*   private fun handshake(socket: Socket, clientReader: BufferedReader, clientWriter: BufferedWriter) : String {
        clientWriter.write(Gson().toJson("Please submit student Id"))
        clientWriter.flush()
        val sidJson = clientReader.readLine()
        val sid = Gson().fromJson(sidJson, ContentModel::class.java)
        updateClientMap(sid.message, socket)

        //Todo student id in class check use helper function
        //Todo encryption exchange use helper function
        return sid.message


    } */

    fun sendMessage(content: ContentModel,clientWriter: BufferedWriter) {
        thread {
            val contentAsStr:String = Gson().toJson(content)
            clientWriter.write("$contentAsStr\n")
            clientWriter.flush()
        }
    }



}