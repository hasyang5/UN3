package org.un.core.services.http;

import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import org.un.common.crypto.SignInterface;
import org.un.common.crypto.SignUtils;
import org.un.common.utils.ByteArray;
import org.un.common.utils.StringUtil;
import org.un.common.utils.Utils;
import org.un.common.utils.WalletUtil;
import org.un.core.Wallet;
import org.un.core.config.args.Args;


@Component
@Slf4j(topic = "API")
public class GenerateAddressServlet extends RateLimiterServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      SignInterface sign = SignUtils.getGeneratedRandomSign(Utils.getRandom(),
          Args.getInstance().isECKeyCryptoEngine());
      byte[] priKey = sign.getPrivateKey();
      byte[] address = sign.getAddress();
      String priKeyStr = Hex.encodeHexString(priKey);
      String base58check = StringUtil.encode58Check(address);
      String hexString = ByteArray.toHexString(address);
      JSONObject jsonAddress = new JSONObject();
      jsonAddress.put("address", base58check);
      jsonAddress.put("hexAddress", hexString);
      jsonAddress.put("privateKey", priKeyStr);
      response.getWriter().println(jsonAddress.toJSONString());
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }
}