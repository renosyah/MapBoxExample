package com.syahputrareno975.mapboxexample

import android.os.AsyncTask
import java.util.concurrent.TimeUnit

class SimpleTask : AsyncTask<Void,Void,Boolean> {
    lateinit var controller : (() -> Unit) -> Unit
    lateinit var onUpdate : (Int) -> Unit
    lateinit var onFinish : () -> Unit

    constructor(controller : (() -> Unit) -> Unit,onUpdate: (Int) -> Unit, onFinish: () -> Unit) : super() {
        this.controller = controller
        this.onUpdate = onUpdate
        this.onFinish = onFinish
    }
    override fun onPreExecute() {
        super.onPreExecute()
        controller.invoke(object : () -> Unit{
            override fun invoke() {
                holder.stop = true
            }
        })
    }

    val holder = Holder()
    override fun doInBackground(vararg params: Void?): Boolean {

        while (!holder.stop){
            holder.value++
            publishProgress()
            TimeUnit.SECONDS.sleep(1)
        }
        return true
    }

    override fun onProgressUpdate(vararg values: Void?) {
        super.onProgressUpdate(*values)
        onUpdate.invoke(holder.value)
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        onFinish.invoke()
    }
    class Holder {
        var stop = false
        var value = 0
    }

}