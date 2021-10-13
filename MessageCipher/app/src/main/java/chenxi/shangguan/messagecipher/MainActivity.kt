package chenxi.shangguan.messagecipher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import chenxi.shangguan.messagecipher.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var ui : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 密码库
        val secLib = SecurityLib()

        // 先看看文件在不在，文件有就开始验证，没有就提示要创建口令
        val authFile = File(filesDir, secLib.authFileName) // 内部储存

        // 获取MAC
        val mac: String? = Utils().getMACAddress("wlan0")

        if (!authFile.exists()) { // 没有验证文件的情况，弹个框框告诉你啥没有
            //AlertDialog.Builder(this).setTitle("提示").setMessage("未创建验证数据，直接输入口令登陆以设定初始口令\n注意：忘记口令您将丢失所有数据").setPositiveButton("好", null).create().show()
            ui.txtHint.setText("未创建口令，输入口令以设定初始口令，建议设置强口令\n\n注意：忘记口令您将丢失所有数据\n\n清除本程序数据可以重置口令但您也将丢失所有数据")
            ui.textView2.setText("创建口令")
            ui.btnAuth.setText("创建")
        }

        ui.btnAuth.setOnClickListener {
            // 什么都没输入当然是什么都不做的啦
            val authCode = ui.txtAuthCode.text.toString() //
            if(authCode == ""){
                ui.txtHint.setText("口令不能为空")
                return@setOnClickListener
            }

            // 用于身份验证的字符串
            /**
             * 这个阶段的密码的处理方式：MD5（MAC地址 + 输入的内容） ==> AES加密用于校验口令的字符串（val CHECK_AUTH_STR = 。。。）
             * */
            val originalStr = mac + authCode
            var thisAesKey = secLib.getStringMD5(originalStr) // 二次口令，用于加密”通讯录“的AES密钥
            //AlertDialog.Builder(this).setTitle("提示").setMessage("二次口令：$thisAesKey").setPositiveButton("好", null).create().show()

            if (!authFile.exists()) { // 没有验证文件的情况，也即是全新的
                if (authFile.createNewFile()) {
                    // ”通讯录“的AES密钥
                    var aesMasterKey = secLib.genAesRandom()

                    // 第一段数据.第二段数据
                    val data = "${secLib.aesEncrypt(secLib.CHECK_AUTH_STR, thisAesKey!!)}.${secLib.aesEncrypt(aesMasterKey!!, thisAesKey)}"
                    aesMasterKey = ""
                    //AlertDialog.Builder(this).setTitle("提示").setMessage("验证数据（加密的）：\n$data").setPositiveButton("好", null).create().show()

                    // 数据写入文件
                    try {
                        val fos = FileOutputStream(authFile)
                        fos.write(data.toByteArray())
                        fos.close()
                        ui.txtAuthCode.setText("")
                        //AlertDialog.Builder(this).setTitle("提示").setMessage("验证数据已创建，重新输入口令以验证").setPositiveButton("好", null).create().show()
                        ui.txtHint.setText("口令已创建，重新输入口令以验证")
                        ui.textView2.setText("身份验证")
                        ui.btnAuth.setText("验证")
                    }catch (e: IOException){
                        //AlertDialog.Builder(this).setTitle("错误").setMessage("验证数据写入失败").setPositiveButton("好", null).create().show()
                        authFile.delete()
                        ui.txtHint.setText("创建口令失败")
                    }

                    thisAesKey = ""
                    /**
                     * 解密的测试部分
                     * 等会要删掉的
                     * */
                    /*
                    val sectors = data.split(".")
                    //解密的第一段数据
                    //解密的第二段数据
                    AlertDialog.Builder(this).setTitle("提示").setMessage("验证数据（解密的：\n${aesDecrypt(sectors[0], thisAesKey)}\n${aesDecrypt(sectors[1], thisAesKey)}").setPositiveButton("好", null).create().show()
                    */

                } else {
                    // 一般不会出现内部储存无法访问的，以防万一
                    //AlertDialog.Builder(this).setTitle("错误").setMessage("无法创建验证数据").setPositiveButton("好", null).create().show()
                    ui.txtHint.setText("无法创建验证数据")
                    return@setOnClickListener
                }
            } else {
                // 有验证文件的情况，当然是要验证啦
                val fos = FileInputStream(authFile)
                val byte = ByteArray(fos.available()) // 读入文件大小的byte之前要确定文件大小
                val len = fos.read(byte)
                val readContent = String(byte, 0, len)
                val sectors = readContent.split(".")
                ui.txtAuthCode.setText("")
                if (secLib.CHECK_AUTH_STR == secLib.aesDecrypt(sectors[0], thisAesKey!!) && sectors.size == 2){
                    // 这里放行
                    // 取出第二段的”通讯录密钥“传到那边的activity（通讯录）
                    var sendAesKey = secLib.aesDecrypt(sectors[1], thisAesKey!!)
                    //AlertDialog.Builder(this).setTitle("提示").setMessage("验证成功").setPositiveButton("好", null).create().show()
                    ui.txtHint.setText("")
                    val i = Intent(this, ContactBookMain::class.java)
                    i.putExtra("MasterAesKey", sendAesKey)
                    sendAesKey = ""
                    thisAesKey = ""
                    startActivity(i)
                }else{
                    // 不行了
                    //AlertDialog.Builder(this).setTitle("错误").setMessage("身份验证失败，可能由于口令错误或者验证数据被损坏").setPositiveButton("好", null).create().show()
                    ui.txtHint.setText("身份验证失败，可能由于口令错误或者验证数据被损坏\n" +
                            "\n" +
                            "注意：忘记口令您将丢失所有数据\n" +
                            "\n" +
                            "清除本程序数据可以重置口令但您也将丢失所有数据")
                }
            }
        }

    }

}