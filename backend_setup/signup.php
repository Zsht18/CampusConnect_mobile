<?php
// signup.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$id_number = isset($_POST['id_number']) ? trim($_POST['id_number']) : '';
$full_name = isset($_POST['full_name']) ? trim($_POST['full_name']) : '';
$username = isset($_POST['username']) ? trim($_POST['username']) : '';
$password = isset($_POST['password']) ? trim($_POST['password']) : '';

if(!empty($email) && !empty($id_number) && !empty($full_name) && !empty($username) && !empty($password)){

    // Validate institutional email
    $blocked_domains = array('gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com', 'live.com', 'icloud.com', 'me.com', 'aol.com', 'mail.com', 'zoho.com', 'proton.me', 'protonmail.com');
    $email_parts = explode('@', $email);
    $email_domain = count($email_parts) > 1 ? strtolower(end($email_parts)) : '';

    if (in_array($email_domain, $blocked_domains) || (strpos($email_domain, '.edu') === false && strpos($email_domain, '.ac.') === false)) {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Institutional/School Emails Only"));
        exit();
    }

    // Check if email, id_number, or username already exists (case-insensitive)
    $checkQuery = "SELECT id, institutional_email, id_number, username FROM mobile_users WHERE LOWER(institutional_email) = LOWER(:email) OR id_number = :id_number OR LOWER(username) = LOWER(:username) LIMIT 1";
    $checkStmt = $conn->prepare($checkQuery);
    $checkStmt->bindParam(':email', $email);
    $checkStmt->bindParam(':id_number', $id_number);
    $checkStmt->bindParam(':username', $username);
    $checkStmt->execute();

    if($checkStmt->rowCount() > 0){
        $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);
        $errorMsg = "Account already exists.";
        if(strcasecmp($existing['institutional_email'], $email) === 0) {
            $errorMsg = "Email is already registered.";
        } else if($existing['id_number'] === $id_number) {
            $errorMsg = "ID Number is already registered.";
        } else if(strcasecmp($existing['username'], $username) === 0) {
            $errorMsg = "Username is already taken. Please choose another.";
        }

        http_response_code(200);
        echo json_encode(array("success" => false, "message" => $errorMsg));
        exit();
    }

    // Hash the password for security
    $password_hash = password_hash($password, PASSWORD_BCRYPT);

    $query = "INSERT INTO mobile_users SET institutional_email=:email, id_number=:id_number, full_name=:full_name, username=:username, password_hash=:password_hash";
    $stmt = $conn->prepare($query);

    $stmt->bindParam(":email", $email);
    $stmt->bindParam(":id_number", $id_number);
    $stmt->bindParam(":full_name", $full_name);
    $stmt->bindParam(":username", $username);
    $stmt->bindParam(":password_hash", $password_hash);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array("success" => true, "message" => "Account created successfully."));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to create account."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Incomplete data. Please fill all fields."));
}
?>