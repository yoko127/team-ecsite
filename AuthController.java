package jp.co.internous.valhalla.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import jp.co.internous.valhalla.model.domain.MstUser;
import jp.co.internous.valhalla.model.form.UserForm;
import jp.co.internous.valhalla.model.mapper.MstUserMapper;
import jp.co.internous.valhalla.model.mapper.TblCartMapper;
import jp.co.internous.valhalla.model.session.LoginSession;


/**
 * 認証に関する処理を行うコントローラー
 * @author インターノウス
 *
 */
@RestController
@RequestMapping("/valhalla/auth")
public class AuthController {
	
	@Autowired
	private MstUserMapper userMapper;
	
	@Autowired
	private TblCartMapper cartMapper;
	
	@Autowired
	private LoginSession loginSession;
	
	private Gson gson = new Gson();
	
	/**
	 * ログイン処理をおこなう
	 * @param f ユーザーフォーム
	 * @return ログインしたユーザー情報(JSON形式)
	 */
	@PostMapping("/login")
	public String login(@RequestBody UserForm f) {
		//DBをもとに認証する
		String userName = f.getUserName();
		String password = f.getPassword(); 
		MstUser user = userMapper.findByUserNameAndPassword(userName, password);
		
		int tmpUserId = loginSession.getTmpUserId();
		// 仮IDでカート追加されていれば、本ユーザーIDに更新する。
		if (user != null && tmpUserId != 0) {
			int count = cartMapper.findCountByUserId(tmpUserId);
			if (count > 0) {
				cartMapper.updateUserId(user.getId(), tmpUserId);
			}
		}
		
		if (user != null) {
			loginSession.setTmpUserId(0);
			loginSession.setLogined(true);
			loginSession.setUserId(user.getId());
			loginSession.setUserName(user.getUserName());
			loginSession.setPassword(user.getPassword());
		} else {
			loginSession.setLogined(false);
			loginSession.setUserId(0);
			loginSession.setUserName(null);
			loginSession.setPassword(null);
		}
		
		return gson.toJson(user);
		
	}
	
	/**
	 * ログアウト処理をおこなう
	 * @return 空文字
	 */
	@PostMapping("/logout")
	public String logout() {
		loginSession.setTmpUserId(0);
		loginSession.setLogined(false);
		loginSession.setUserId(0);
		loginSession.setUserName(null);
		loginSession.setPassword(null);
		return "";
	}
	
	/**
	 * パスワード再設定をおこなう
	 * @param f ユーザーフォーム
	 * @return 処理後のメッセージ
	 */
	@PostMapping("/resetPassword")
	public String resetPassword(@RequestBody UserForm f) {
		String newPassword = f.getNewPassword();
		//入力チェックをおこないエラーが発生した場合は、アラートを表示する
		if (newPassword.length() < 6 || newPassword.length() > 16) {
	    	return "hasError";
		}  else {
		//エラーがない場合は、DBの会員情報マスタテーブルのパスワードとセッションのパスワードを入力値で更新する。  
		String userName = f.getUserName();
		userMapper.updatePassword(userName, newPassword);
		loginSession.setPassword(f.getNewPassword());
		}
		//更新成功した場合は、アラートで「パスワードが再設定されました。」を表示し、マイページのパスワードを変更する。
		return "パスワードが再設定されました。";
		
	}
	
}
