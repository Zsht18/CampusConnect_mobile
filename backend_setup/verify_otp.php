<?php
// verify_otp.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$otp_code = isset($_POST['otp_code']) ? trim($_POST['otp_code']) : '';

if(!empty($email) && !empty($otp_code)){
    $query = "SELECT * FROM mobile_otps WHERE email = :email AND otp_code = :otp_code AND expires_at >= NOW() LIMIT 1";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':email', $email);
    $stmt->bindParam(':otp_code', $otp_code);
    $stmt->execute();

    if($stmt->rowCount() > 0){
        $delQuery = "DELETE FROM mobile_otps WHERE email = :email";
        $delStmt = $conn->prepare($delQuery);
        $delStmt->bindParam(':email', $email);
        $delStmt->execute();

        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Email verified successfully."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Invalid or expired OTP code."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Please provide email and OTP code."));
}
?>