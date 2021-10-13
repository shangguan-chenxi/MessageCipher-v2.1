package chenxi.shangguan.messagecipher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import chenxi.shangguan.messagecipher.databinding.ActivityAesCipherBinding
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import androidx.appcompat.app.AlertDialog
import java.lang.Byte
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesCipher : AppCompatActivity() {

    private lateinit var ui : ActivityAesCipherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_aes_cipher)
        ui = ActivityAesCipherBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 当前Activity加入到链表
        ExitApplication.instance!!.addActivity(this)

        // 密码库
        val secLib = SecurityLib()

        ui.textViewContact.setText(intent.getStringExtra("Contact"))
        val communication_key = intent.getStringExtra("CommunicationKey")

        // 返回上一activity
        ui.btnBack3.setOnClickListener {
            finish()
        }

        ui.btnEncrypt.setOnClickListener {
            val plainText = ui.plainText.text.toString()

            if (plainText == ""){
                //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
                ui.textViewPrompt.setText("需要提供明文")
            }else if(communication_key == ""){
                //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密码").setPositiveButton("好" , null ).create().show()
                ui.textViewPrompt.setText("通讯密钥不能为空")
            }else if (communication_key.length == 16 || communication_key.length == 24 || communication_key.length == 32){
                val encryptedText = secLib.aesEncrypt(plainText, communication_key)
                if (encryptedText == null) {
                    //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("发生未知错误").setPositiveButton("好" , null ).create().show()
                    ui.textViewPrompt.setText("加密时发生未知错误")
                }else{
                    ui.plainText.text.clear()
                    ui.encryptedText.text.clear()
                    ui.encryptedText.setText(encryptedText)

                    // 将内容复制到剪贴板
                    try {
                        //获取剪贴板管理器
                        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        // 创建普通字符型ClipData
                        val mClipData = ClipData.newPlainText("Label", ui.encryptedText.text)
                        // 将ClipData内容放到系统剪贴板里。
                        cm.setPrimaryClip(mClipData);

                    } catch (e: Exception) {

                    }
                    ui.textViewPrompt.setText("消息已加密并已复制到剪贴板")
                }
            }else{
                //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
                ui.textViewPrompt.setText("通讯密钥应为16/24/32字符长度")
            }
        }

        ui.btnDecrypt.setOnClickListener {
            val encryptedText = ui.encryptedText.text.toString()

            if (encryptedText == ""){
                //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
                ui.textViewPrompt.setText("需要提供密文")
            }else if(communication_key == ""){
                //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密码").setPositiveButton("好" , null ).create().show()
                ui.textViewPrompt.setText("通讯密钥不能为空")
            }else if (communication_key.length == 16 || communication_key.length == 24 || communication_key.length == 32){
                val decryptedText = secLib.aesDecrypt(encryptedText, communication_key)
                if (decryptedText == null){
                    //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("发生未知错误").setPositiveButton("好" , null ).create().show()
                    ui.textViewPrompt.setText("解密时发生未知错误")
                }else{
                    ui.plainText.text.clear()
                    ui.plainText.setText(decryptedText)
                    ui.textViewPrompt.setText("消息已被解密")
                }
            }else{
                //AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
                ui.textViewPrompt.setText("通讯密钥应为16/24/32字符长度")
            }
        }

        ui.btnDelPlainTxt.setOnClickListener {
            ui.plainText.text.clear()
            ui.textViewPrompt.setText("已清空明文")
        }

        ui.btnPurgeAll.setOnClickListener {
            ui.plainText.text.clear()
            ui.encryptedText.text.clear()

            // 清空剪贴板
            try{
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val mClipData = ClipData.newPlainText("", "")
                cm.setPrimaryClip(mClipData)
            }catch (e: Exception){

            }

            //AlertDialog.Builder(this@AesCipher).setTitle("提示").setMessage("重置成功").setPositiveButton("好" , null ).create().show()
            ui.textViewPrompt.setText("已清空文本域")
        }

        ui.butDelEncryptedTxt.setOnClickListener {
            ui.encryptedText.text.clear()
            ui.textViewPrompt.setText("已清空密文")
        }

        ui.btnPasteEncryptedTxt.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.encryptedText.text.clear()
                    ui.encryptedText.setText(pasteString)

                    ui.btnDecrypt.callOnClick()
                }
            }catch (e: Exception){

            }
        }

        ui.btnExitNow.setOnClickListener {
            ExitApplication.instance?.exit()
        }

    }

}