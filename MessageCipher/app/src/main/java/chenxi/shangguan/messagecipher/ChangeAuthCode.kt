@file:Suppress("DEPRECATION")

package chenxi.shangguan.messagecipher

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import chenxi.shangguan.messagecipher.databinding.ActivityChangeAuthCodeBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ChangeAuthCode : AppCompatActivity() {

    private lateinit var ui: ActivityChangeAuthCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        ui = ActivityChangeAuthCodeBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 当前Activity加入到链表
        ExitApplication.instance!!.addActivity(this)

        // 密码库
        val secLib = SecurityLib()

        // 先看看文件在不在，文件有就开始验证，没有就提示要创建口令
        val authFile = File(filesDir, secLib.authFileName) // 内部储存

        // 获取MAC
        val mac: String? = Utils().getMACAddress("wlan0")

        // 返回上一activity
        ui.btnBack2.setOnClickListener {
            finish()
        }

        /**
         * 修改密码，旧密码解密然后新密码加密 验证文件的内容
         * */
        ui.btnChange.setOnClickListener {
            val oldCode = ui.txtOldAuthCode.text.toString()
            val newCode = ui.txtNewAuthCode.text.toString()
            val newReCode = ui.txtReNewAuthCode.text.toString()
            if (oldCode == ""){
                ui.prompt.setText("'原口令' 不能为空")
            }else if(newCode == ""){
                ui.prompt.setText("'新口令' 不能为空")
            }else if(newReCode == ""){
                ui.prompt.setText("'校验新口令' 不能为空")
            }else if(newCode != newReCode){
                ui.prompt.setText("'新口令' 和 '校验新口令' 不匹配")
            }else{
                val fos = FileInputStream(authFile)
                val byte = ByteArray(fos.available()) // 读入文件大小的byte之前要确定文件大小
                val len = fos.read(byte)
                val readContent = String(byte, 0, len)
                val sectors = readContent.split(".")

                val originalStr = mac + oldCode
                var thisAesKey = secLib.getStringMD5(originalStr)

                if (secLib.CHECK_AUTH_STR == secLib.aesDecrypt(sectors[0], thisAesKey!!) && sectors.size == 2){
                    val newStr = mac + newCode
                    val newAuthKey = secLib.getStringMD5(newStr)
                    val MasterAesKey = secLib.aesDecrypt(sectors[1], thisAesKey)
                    val newAuthString = "${secLib.aesEncrypt(secLib.CHECK_AUTH_STR, newAuthKey!!)}.${secLib.aesEncrypt(MasterAesKey!!, newAuthKey)}"

                    val f = FileOutputStream(authFile)
                    f.write(newAuthString.toByteArray())
                    f.close()

                    AlertDialog.Builder(this).setTitle("提示").setMessage("口令修改成功").setPositiveButton("好", DialogInterface.OnClickListener { dialogInterface, i ->
                        ExitApplication.instance!!.exit()
                    }).setOnDismissListener{
                        ExitApplication.instance!!.exit()
                    }.create().show()
                }else{
                    ui.prompt.setText("错误的原口令")
                    ui.txtOldAuthCode.setText("")
                }
            }

        }

        /**
         * 清空
         * */
        ui.btnClear.setOnClickListener {
            ui.txtOldAuthCode.text.clear()
            ui.txtNewAuthCode.text.clear()
            ui.txtReNewAuthCode.text.clear()
            ui.prompt.setText("已清空")
        }

        /**
         * 退出程序
         * */
        ui.btnExit.setOnClickListener {
            ExitApplication.instance?.exit()
        }
    }
}