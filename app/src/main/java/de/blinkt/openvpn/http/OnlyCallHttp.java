package de.blinkt.openvpn.http;

import com.google.gson.Gson;
import cn.com.johnson.model.OnlyCallModel;
import de.blinkt.openvpn.constant.HttpConfigUrl;

/**
 * Created by Administrator on 2016/9/10 0010.
 */
public class OnlyCallHttp extends BaseHttp {

	private OnlyCallModel model;

	public OnlyCallModel getOnlyCallModel() {
		return model;
	}

	public OnlyCallHttp(InterfaceCallback callback, int cmdType) {
		super(callback,cmdType,false,GET_MODE,HttpConfigUrl.GET_MAX_PHONE_CALL_TIME);
	}



	@Override
	protected void parseObject(String response) {
		model = new Gson().fromJson(response, OnlyCallModel.class);
	}


}
