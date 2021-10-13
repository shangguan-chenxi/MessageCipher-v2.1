package chenxi.shangguan.messagecipher

import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurityLib {

    // 一个用于校验口令的字符串
    val CHECK_AUTH_STR = "MessageCipher_V2.02_AUTH_STR"

    // 各种文件位置
    /**
     * 这个文件的数据结构：{"二次口令"AES加密 的 校验口令}.{二次口令"AES加密" 的 "通讯录密钥"}
     * */
    val authFileName = "auth.txt"

    /**
     * 这个文件的数据结构：{明文联系人名称}.{"通讯录密钥"AES加密(己方RSA公钥.己方RSA私钥.对方RSA公钥)}\n
     * */
    val contactFileName = "contact.txt"

    /**
     * 发送的消息 数据结构： [密码用对方公钥加密].[密码用己方公钥加密].[签名].[密文内容]
     */

    /**
     * =============================================================================================
     * =============================================================================================
     * =============================================================================================
     * AES函数和配置部分
     * =============================================================================================
     * =============================================================================================
     * =============================================================================================
     * */

    //private static final String CipherMode = "AES/ECB/PKCS5Padding";使用ECB加密，不需要设置IV，但是不安全
    private val CipherMode = "AES/CFB/NoPadding" //使用CFB加密，需要设置IV

    /**
     * AES对字符串加密
     *
     * @param key  密钥
     * @param data 源字符串
     * @return 加密后的字符串
     */
    @Throws(Exception::class)
    fun aesEncrypt(data: String, key: String): String? {
        return try {
            val cipher = Cipher.getInstance(CipherMode)
            val keyspec = SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, IvParameterSpec(ByteArray(cipher.blockSize)))
            val encrypted = cipher.doFinal(data.toByteArray())
            byteToHex(encrypted) // HEX
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * AES对字符串解密
     *
     * @param key  密钥
     * @param data 已被加密的字符串
     * @return 解密得到的字符串
     */
    @Throws(Exception::class)
    fun aesDecrypt(data: String, key: String): String? {
        return try {
            val encrypted1 = hexToByte(data) // HEX
            val cipher = Cipher.getInstance(CipherMode)
            val keyspec = SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(Cipher.DECRYPT_MODE, keyspec, IvParameterSpec(ByteArray(cipher.blockSize)))
            val original = cipher.doFinal(encrypted1)
            String(original, charset("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 这里是自编的HEX转换部分 */
    // 应该是没错的，错了就不知道了，反正不依赖库函数了，怕了
    private val hexArray = "0123456789ABCDEF".toCharArray() // HEX的16个字符，废话。。。不然是什么

    /**
     * hex => byteArray
     *
     */
    fun hexToByte(hex: String?): ByteArray? {
        var m = 0
        var n = 0
        val byteLen = hex!!.length / 2 // 2个HEX字符描述一个Byte
        val ret = ByteArray(byteLen)
        for (i in 0 until byteLen) {
            m = i * 2 + 1
            n = m + 1
            val intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.subSequence(m, n))
            ret[i] = java.lang.Byte.valueOf(intVal.toByte())
        }
        return ret
    }

    /**
     * ByteArray => hex
     *
     */
    fun byteToHex(bytes: ByteArray?): String? {
        val hexChars = CharArray(bytes!!.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4] // 向右边位移4
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    /** 随机32位AES密码 */
    fun genAesRandom(): String? {
        var retStr = ""
        val strTable =
                "`1234567890-=abcdefghijklmnopqrstuvwxyz~!@#$%^&*()_+[]{}ABCDEFGHIJKLMNOPQRSTUVWXYZ;:'\",./<>?|\\" // 所有能输入成文本的字符都来一个就是了
        val len = strTable.length
        var bDone = true
        do {
            retStr = ""
            var count = 0
            for (i in 0 until 32) {
                val dblR = Math.random() * len
                val intR = Math.floor(dblR).toInt()
                val c = strTable[intR]
                if (c in '0'..'9') {
                    count++
                }
                retStr += strTable[intR]
            }
            // 破损字符处理，重来得了
            if (count >= 2) {
                bDone = false
            }
        } while (bDone)
        return retStr
    }


    /**
     * =============================================================================================
     * =============================================================================================
     * =============================================================================================
     * RSA函数和配置部分
     * =============================================================================================
     * =============================================================================================
     * =============================================================================================
     * */

    /**
     * var keys: Map<String, Any>?
     * var v1: String? // 公钥
     * var v2: String? // 私钥
     * */

    val KEY_ALGORITHM = "RSA" // 加密算法
    val ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding" // 填充方式
    val SIGNATURE_ALGORITHM = "MD5withRSA" // 签名算法

    val PUBLIC_KEY = "RSAPublicKey"
    val PRIVATE_KEY = "RSAPrivateKey"

    // 不用写，上一个版本写好了有的。。。调试了很久的一套东西，复制粘贴就有了
    // 函数稍微改改

    /**
     * 用私钥对信息生成数字签名
     *
     * @param data       加密数据
     * @param privateKey 私钥
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sign(data: String?, privateKey: String?): String? {
        // 解密由base64编码的私钥
        val keyBytes = hexToByte(privateKey)
        try {
            // 构造PKCS8EncodedKeySpec对象
            val pkcs8KeySpec = PKCS8EncodedKeySpec(keyBytes)

            // KEY_ALGORITHM 指定的加密算法
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)

            // 取私钥匙对象
            val priKey = keyFactory.generatePrivate(pkcs8KeySpec)

            // 用私钥对信息生成数字签名
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initSign(priKey)
            signature.update(data?.toByteArray(charset("utf-8")))
            return byteToHex(signature.sign())
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 校验数字签名
     *
     * @param data      加密数据
     * @param publicKey 公钥
     * @param sign      数字签名
     * @return 校验成功返回true 失败返回false
     * @throws Exception
     */
    @Throws(Exception::class)
    fun verify(data: String?, publicKey: String?, sign: String?): Boolean {
        try {
            // 解密由base64编码的公钥
            val keyBytes = hexToByte(publicKey)

            // 构造X509EncodedKeySpec对象
            val keySpec = X509EncodedKeySpec(keyBytes)

            // KEY_ALGORITHM 指定的加密算法
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)

            // 取公钥匙对象
            val pubKey = keyFactory.generatePublic(keySpec)
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(pubKey)
            signature.update(data?.toByteArray(charset("utf-8")))
            // 验证签名是否正常
            return signature.verify(hexToByte(sign))
        }catch (e: Exception){
            return false
        }
    }

    /**
     * 解密<br></br>
     * 用私钥解密
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun decryptByPrivateKey(data: String?, key: String?): String? {
        try {
            // 对密钥解密
            val keyBytes = hexToByte(key)

            // 取得私钥
            val pkcs8KeySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val privateKey: Key = keyFactory.generatePrivate(pkcs8KeySpec)

            // 对数据解密
            //val cipher = Cipher.getInstance(keyFactory.algorithm)
            val cipher = Cipher.getInstance(ECB_PKCS1_PADDING)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val original = cipher.doFinal(hexToByte(data))  // HEX
            return String(original, charset("UTF-8"))
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 解密<br></br>
     * 用公钥解密
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun decryptByPublicKey(data: String?, key: String?): String? {
        try {
            // 对密钥解密
            val keyBytes = hexToByte(key)

            // 取得公钥
            val x509KeySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val publicKey: Key = keyFactory.generatePublic(x509KeySpec)

            // 对数据解密
            //val cipher = Cipher.getInstance(keyFactory.algorithm)
            val cipher = Cipher.getInstance(ECB_PKCS1_PADDING)
            cipher.init(Cipher.DECRYPT_MODE, publicKey)
            val original = cipher.doFinal(hexToByte(data))
            return String(original, charset("UTF-8"))
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 加密<br></br>
     * 用公钥加密
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun encryptByPublicKey(data: String?, key: String?): String? {
        try {
            // 对公钥解密
            val keyBytes = hexToByte(key)
            // 取得公钥
            val x509KeySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val publicKey: Key = keyFactory.generatePublic(x509KeySpec)

            // 对数据加密
            //val cipher = Cipher.getInstance(keyFactory.algorithm)
            val cipher = Cipher.getInstance(ECB_PKCS1_PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val original = cipher.doFinal(data?.toByteArray())
            return byteToHex(original) // HEX
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 加密<br></br>
     * 用私钥加密
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun encryptByPrivateKey(data: String?, key: String?): String? {
        try {
            // 对密钥解密
            val keyBytes = hexToByte(key)

            // 取得私钥
            val pkcs8KeySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val privateKey: Key = keyFactory.generatePrivate(pkcs8KeySpec)

            // 对数据加密
            //val cipher = Cipher.getInstance(keyFactory.algorithm)
            val cipher = Cipher.getInstance(ECB_PKCS1_PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, privateKey)
            val original = cipher.doFinal(data?.toByteArray())
            return byteToHex(original) // HEX
        }catch (e: Exception){
            return ""
        }
    }

    /**
     * 取得私钥
     *
     * @param keyMap
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getPrivateKey(keyMap: Map<String, Any>): String? {
        val key = keyMap[PRIVATE_KEY] as Key?
        return byteToHex(key!!.encoded)
    }

    /**
     * 取得公钥
     *
     * @param keyMap
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getPublicKey(keyMap: Map<String, Any>): String? {
        val key = keyMap[PUBLIC_KEY] as Key?
        return byteToHex(key!!.encoded)
    }

    /**
     * 初始化密钥
     *
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun initKey(): Map<String, Any>? {
        val keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        // RSA算法要求有一个可信任的随机数源
        val secureRandom = SecureRandom()

        //keyPairGen.initialize(2048)
        keyPairGen.initialize(2048, secureRandom)

        val keyPair = keyPairGen.generateKeyPair()
        // 公钥
        val publicKey = keyPair.public as RSAPublicKey
        // 私钥
        val privateKey = keyPair.private as RSAPrivateKey
        val keyMap: MutableMap<String, Any> = HashMap(2)
        keyMap[PUBLIC_KEY] = publicKey
        keyMap[PRIVATE_KEY] = privateKey
        return keyMap
    }

    /**
     * =============================================================================================
     * =============================================================================================
     * =============================================================================================
     * MD5
     * =============================================================================================
     * =============================================================================================
     * =============================================================================================
     * */

    fun getStringMD5(str : String?): String?{
        val instance = MessageDigest.getInstance("MD5")
        return byteToHex(instance.digest(str!!.toByteArray()))
    }

}