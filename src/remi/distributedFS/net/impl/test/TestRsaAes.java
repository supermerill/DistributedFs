package remi.distributedFS.net.impl.test;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import remi.distributedFS.util.ByteBuff;

public class TestRsaAes {
	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		
		SecretKey secretKey = null;
			//create new aes key
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			secretKey = keyGen.generateKey();
			
			
			byte[] datakey = secretKey.getEncoded();

			System.out.println(secretKey.getAlgorithm());
			System.out.println(secretKey.getFormat());
			
			ByteBuff msg = new ByteBuff().putUTF8("lol c mon messsage");
			System.out.println(msg.rewind().getUTF8());

			Cipher aesCipher = Cipher.getInstance("AES");
			aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] byteCipherText = aesCipher.doFinal(msg.array());

			SecretKey sk2 = new SecretKeySpec(datakey, 0, datakey.length, "AES"); 
			
//			aesCipher.init(Cipher.DECRYPT_MODE, secretKey, aesCipher.getParameters());
			Cipher aesCipher2 = Cipher.getInstance("AES");
			aesCipher2.init(Cipher.DECRYPT_MODE, sk2);
			byte[] byteDecryptedText = aesCipher2.doFinal(byteCipherText);
			System.out.println(new ByteBuff(byteDecryptedText).getUTF8());

//			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("AES");
//			PKCS8EncodedKeySpec bobPrivKeySpec = new PKCS8EncodedKeySpec(datakey);
//			System.out.println(keyFactory.getProvider());

//			KeyGenerator keyGen2 = KeyGenerator.getInstance("AES");
//			keyGen2.
			
			
//			SecretKey decodedKey = keyFactory.(keySpec)(bobPrivKeySpec);
			
	}
}
