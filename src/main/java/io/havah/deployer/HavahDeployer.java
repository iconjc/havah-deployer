package io.havah.deployer;

import foundation.icon.icx.*;
import foundation.icon.icx.crypto.KeystoreException;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcArray;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.math.BigInteger;
import java.util.Iterator;


public class HavahDeployer {

    public static void main(String[] args) {
        String content = null, json = null;
        if(args.length != 2) {
            System.out.println("Usage: havahdeployer PARAMS_JSON_FILE SCORE_ZIP_FILE");
            return;
        } else {
            for(int i=0; i<args.length;i++) {
                switch (i) {
                    case 0:
                        json = args[i];
                        System.out.println("PARAMS_JSON_FILE : " + json);
                        break;
                    case 1:
                        content = args[i];
                        System.out.println("SCORE_ZIP_FILE : " + content);
                        break;
                }
            }
        }

        if(json == null || content == null) {
            System.out.println("Usage: havahdeployer deploy SCORE_ZIP_FILE");
            return;
        }

        Reader reader = null;
        try {
            reader = new FileReader(json);
        } catch (FileNotFoundException e) {
            System.out.println("Error : file not found : " + json);
            return;
        }

        JSONParser jsonParser = new JSONParser();

        try {
            JSONObject jsonObject = (JSONObject)jsonParser.parse(reader);

            File file = new File(content);
            byte[] bytes = null;
            try {
                bytes = Utils.readBytes(file);
            } catch (IOException e) {
                System.out.println("Error : can not read : " + content);
                return;
            }

            Wallet wallet = null;
            try {
                file = new File((String) jsonObject.get("keystore"));
                wallet = KeyWallet.load((String) jsonObject.get("keypass"), file);
            } catch (KeystoreException | IOException e) {
                System.out.println("Error: can not load wallet");
                return;
            }

            System.out.println("uri: " + jsonObject.get("uri"));
            IconService iconService = new IconService(new HttpProvider((String) jsonObject.get("uri")));

            RpcObject rpcParams = json2Rpc((JSONObject) jsonObject.get("params"));

            BigInteger nid = null;
            if (((String) jsonObject.get("nid")).startsWith("0x")) {
                nid = new BigInteger(((String) jsonObject.get("nid")).substring(2), 16);
            } else {
                nid = new BigInteger(((String) jsonObject.get("nid")));
            }

            Address to = new Address(jsonObject.get("to") != null ? (String) jsonObject.get("to") : "cx0000000000000000000000000000000000000000");

            BigInteger value = jsonObject.get("value") != null ? new BigInteger((String) jsonObject.get("value")) : BigInteger.ZERO;

            Transaction transaction = TransactionBuilder.newBuilder()
                    .nid(nid)
                    .from(wallet.getAddress())
                    .to(to)
                    .value(value)
                    .stepLimit(new BigInteger((String) jsonObject.get("steplimit")))
                    .deploy("application/java", bytes)
                    .params(rpcParams)
                    .build();

            SignedTransaction signedTransaction = new SignedTransaction(transaction, wallet);

            Request<Bytes> request = iconService.sendTransaction(signedTransaction);
            try {
                Bytes txHash = request.execute();
                TransactionResult result = Utils.getTransactionResult(iconService, txHash);
                if(result.getStatus().compareTo(BigInteger.ZERO) == 0) {
                    System.out.println("Error: result failed : " + result);
                    return;
                }
                System.out.println("Deploy Success : " + result.getScoreAddress());
            } catch (Exception e) {
                System.out.println("Error: deploy failed : " + e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            System.out.println("Error: json parse error : " + e);
        }
    }

    public static RpcObject json2Rpc(JSONObject json) {
        RpcObject.Builder builder = new RpcObject.Builder();

        Iterator<String> iter = json.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            Object value = json.get(key);
            if(value instanceof String) {
                builder.put(key, new RpcValue((String) value));
            } else if(value instanceof JSONArray) {
                var array = new RpcArray.Builder();
                for (Object object : (JSONArray) value) {
                    array.add(json2Rpc((JSONObject) object));
                }
                builder.put(key, array.build());
            }
        }

        return builder.build();
    }
}
