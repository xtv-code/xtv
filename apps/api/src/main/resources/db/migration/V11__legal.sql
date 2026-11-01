CREATE TABLE legal_document (
  code VARCHAR(16) NOT NULL PRIMARY KEY,
  body LONGTEXT NOT NULL
);

INSERT INTO legal_document (code, body) VALUES
('PRIVACY', '{"zh":"账号、订单和播放记录只用于提供会员与单集购买服务。我们不会出售这些信息。","en":"Account, order, and playback records are used only to provide membership and episode purchases. We do not sell this information.","ja":"アカウント、注文、再生の記録は、会員と単話購入の提供にのみ使います。これらの情報を販売することはありません。","ko":"계정, 주문, 재생 기록은 멤버십과 회차 구매를 제공하는 데만 사용합니다. 이 정보를 판매하지 않습니다."}'),
('TERMS', '{"zh":"会员和已购影片在支付完成后即可观看。虚拟内容开通后不支持退款。","en":"Membership and purchased videos are available after payment. Virtual access is not refundable once it is opened.","ja":"会員と購入した動画は、支払い完了後に視聴できます。仮想コンテンツの開通後は返金できません。","ko":"멤버십과 구매한 영상은 결제가 끝나면 볼 수 있습니다. 가상 이용권은 열린 뒤 환불되지 않습니다."}');
