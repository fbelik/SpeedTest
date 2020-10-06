package com.bignerdranch.android.speedtest

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.net.*
import java.io.*
import java.lang.Exception

const val CHUNK_SIZE = 4194399
class MainActivity : AppCompatActivity() {

    private lateinit var ipAddr: EditText
    private lateinit var portNum: EditText
    private lateinit var numChunks: EditText
    private lateinit var goStop: Button
    private lateinit var ctChunks: TextView
    private lateinit var netSpeed: TextView
    private lateinit var errorMsg: TextView
    private var currentTask: AsyncTask<String,Int,String>? = null
    private var deltaTime = 0L
    private var ip = ""
    private var port = 0
    private var chunks = 0
    private var error = ""
    private var isError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipAddr = findViewById(R.id.ip_addr)
        portNum = findViewById(R.id.port_number)
        numChunks = findViewById(R.id.num_chunks)
        numChunks.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (numChunks.text.isNotEmpty()) {
                    try {
                        numChunks.text.toString().toInt()
                    } catch (e: java.lang.NumberFormatException) {
                        numChunks.setText(numChunks.text.slice(0 until numChunks.text.length - 1))
                        numChunks.setSelection(numChunks.text.length)
                    }
                }
                if (numChunks.text.length > 4) {
                    numChunks.setText(numChunks.text.slice(0 until 4))
                    numChunks.setSelection(numChunks.text.length)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        goStop = findViewById(R.id.go_stop_btn)
        ctChunks = findViewById(R.id.num_chunks_recieved)
        netSpeed = findViewById(R.id.network_speed)
        errorMsg = findViewById(R.id.error_msg)

        class NetworkTest: AsyncTask<String, Int, String>() {

            private var client: Socket? = null

            override fun doInBackground(vararg params: String?): String {
                try {
                    val client = Socket(ip, port)

                    val dIS = DataInputStream(client.getInputStream())
                    val dOS = DataOutputStream(client.getOutputStream())

                    val byteBuffer = ByteArray(CHUNK_SIZE)

                    dOS.writeInt(chunks) // Chunks

                    val start = System.currentTimeMillis()

                    for (i in 0 until chunks) {
                        var ct = 0
                        while (ct < CHUNK_SIZE) {
                            ct += dIS.read(byteBuffer, ct, CHUNK_SIZE - ct)
                        }
                        publishProgress(i+1)
                    }
                    val end = System.currentTimeMillis()
                    deltaTime = (end - start) * 1000
                    return "Task Completed."
                }
                catch (e: Exception) {
                    error = e.toString()
                    isError = true
                    return "Error"
                }
            }

            override fun onProgressUpdate(vararg values: Int?) {
                ctChunks.text = "%d".format(values[0])
            }

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                currentTask = null
                goStop.text = "Go"
                if (!isError) {
                    val networkSpeed = (8.0 * CHUNK_SIZE * chunks) / (deltaTime)
                    netSpeed.text = "%.2f".format(networkSpeed)
                }
                else {
                    errorMsg.text = error
                }
            }
        }

        goStop.setOnClickListener {
            if (currentTask == null) {
                goStop.text = "Stop"
                ctChunks.text="0"
                var cont = true
                try {
                    ip = ipAddr.text.toString()
                    port = portNum.text.toString().toInt()
                    chunks = numChunks.text.toString().toInt()
                }
                catch (e: Exception) {
                    errorMsg.text = e.toString()
                    cont = false
                    goStop.text = "Go"
                }
                if (cont) {
                    currentTask = NetworkTest()
                    currentTask?.execute()
                    errorMsg.text = ""
                }
            }
            else {
                currentTask?.cancel(true)
                currentTask = null
                goStop.text = "Go"
            }
        }
    }
}