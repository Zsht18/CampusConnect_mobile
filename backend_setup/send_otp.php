<?php
// send_otp.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';
include_once 'account_status.php';

$email = isset($_POST['email']) ? trim($_POST['email']) : '';

if(!empty($email)){
    // Validate institutional email
    $blocked_domains = array('gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com', 'live.com', 'icloud.com', 'me.com', 'aol.com', 'mail.com', 'zoho.com', 'proton.me', 'protonmail.com');
    $email_parts = explode('@', $email);
    $email_domain = count($email_parts) > 1 ? strtolower(end($email_parts)) : '';

    if (in_array($email_domain, $blocked_domains) || (strpos($email_domain, '.edu') === false && strpos($email_domain, '.ac.') === false)) {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Institutional/School Emails Only"));
        exit();
    }

    $restriction = cc_account_restriction($conn, $email);
    if($restriction !== ''){
        cc_restriction_response($restriction);
    }

    $otp_code = str_pad(rand(0, 999999), 6, '0', STR_PAD_LEFT);
    $expires_at = date('Y-m-d H:i:s', strtotime('+10 minutes'));

    // Save or update OTP in mobile_otps
    $query = "INSERT INTO mobile_otps (email, otp_code, expires_at) VALUES (:email, :otp_code, :expires_at) ON DUPLICATE KEY UPDATE otp_code = :otp_code_upd, expires_at = :expires_at_upd";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':email', $email);
    $stmt->bindParam(':otp_code', $otp_code);
    $stmt->bindParam(':expires_at', $expires_at);
    $stmt->bindParam(':otp_code_upd', $otp_code);
    $stmt->bindParam(':expires_at_upd', $expires_at);
    $stmt->execute();

    $subject = "CampusConnect Email Verification OTP";
    $message = "Your CampusConnect verification code is: " . $otp_code . "\n\nThis code will expire in 10 minutes.";
    $headers = "From: noreply@campusconnect.uslsbsit.com";

    @mail($email, $subject, $message, $headers);

    http_response_code(200);
    echo json_encode(array("success" => true, "message" => "OTP code sent to " . $email, "dev_otp" => $otp_code));
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Please enter an email address."));
}
?>