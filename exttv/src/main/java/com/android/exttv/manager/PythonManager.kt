package com.android.exttv.manager

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.exttv.manager.SectionManager
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.android.exttv.manager.AddonManager as Addons
import com.android.exttv.manager.SectionManager as Sections
import com.android.exttv.manager.StatusManager as Status

object PythonManager {
    private var exttv: PyObject? = null

    fun init(context: Activity) {
        if (exttv != null && Status.sectionList.isNotEmpty()) {
            return
        }

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        val runnable = Runnable {
            val py = Python.getInstance()
            exttv = py.getModule("exttv") // this initialize the workspace
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()
    }

    fun addPluginFromGit() {//owner: String, repo: String, branch: String) {
        val owner = "kodiondemand"
        val repo = "addon"
        val branch = "master"

        var pluginName = ""
        val runnable = Runnable {
            val py = Python.getInstance()
            val utils = py.getModule("utils")
            pluginName = utils.callAttr("get_from_git", owner, repo, branch, true).toString()
            Addons.add(pluginName)
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()
        selectAddon(pluginName)
    }

    fun addPluginFromRepository(url: String, pluginName: String){
        val runnable = Runnable {
            val py = Python.getInstance()
            val utils = py.getModule("utils")
            utils.callAttr("get_from_repository", url, pluginName, true)
            Addons.add(pluginName)
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()
        selectAddon(pluginName)
    }

    fun selectAddon(pluginName: String) {
        Log.d("Python", pluginName)
        Sections.removeAllSection()
        Status.titleMap.clear()

        val runnable = Runnable {
            val py = Python.getInstance()
            val utils = py.getModule("utils")
            utils.callAttr("set_plugin_name", pluginName)
        }

        val thread = Thread(runnable)
        thread.start()
        thread.join()

        Addons.selectAddon(pluginName)
        Status.titleMap["plugin://$pluginName/"] = "Menu"
        selectSection("plugin://$pluginName/")
    }

    fun selectSection(argv2: String, sectionIndex: Int = -1, cardIndex: Int = 0) {
        Status.loadingState = LoadingStatus.SECTION
        val runnable = Runnable {
            val title : String = Status.titleMap.getOrDefault(argv2, "")
            // returned value from exttv.py
            val newSection = Section(title, exttv?.callAttr("run", argv2)?.toJava(List::class.java) as List<SectionManager.CardView>)
            Status.titleMap.putAll(newSection.movieList.associate { it.id to it.label })

            if(newSection.movieList.isNotEmpty() && Sections.removeAndAdd(sectionIndex+1, argv2, newSection)) {
                Sections.updateSelectedSection(sectionIndex, cardIndex)
            }

            try {
                Handler(Looper.getMainLooper()).post {
                    if(sectionIndex==-1 && newSection.movieList.isEmpty()){
                        Sections.removeAllSection()
                        Status.bgImage = ""
                    }
                    Status.sectionList = Sections.getSectionsInOrder()
                    Status.loadingState = LoadingStatus.SECTION_DONE
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        Thread(runnable).start()
    }
}
