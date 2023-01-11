package io.havah.deployer;

import foundation.icon.icx.IconService;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcError;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {

    public static TransactionResult getTransactionResult(IconService iconService, Bytes txHash) throws IOException {
        TransactionResult result = null;
        while (result == null) {
            try {
                result = iconService.getTransactionResult(txHash).execute();
            } catch (RpcError e) {
                System.out.println("RpcError: code: " + e.getCode() + ", message: " + e.getMessage());
                try {
                    // wait until block confirmation
                    System.out.println("Sleep 1.2 second.");
                    Thread.sleep(1200);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        return result;
    }

    public static byte[] readBytes(File file) throws IOException {
        long length = file.length();
        if (length > Integer.MAX_VALUE) throw new OutOfMemoryError("File is too big!!");
        byte[] result = new byte[(int) length];
        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(file))) {
            inputStream.readFully(result);
        }
        return result;
    }
}