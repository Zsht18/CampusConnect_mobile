<?php
// login.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$email = isset($_POST['email']) ? $_POST['email'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if(!empty($email) && !empty($password)){
    // Fetch user from database and count their posts
    $query = "SELECT u.id, u.institutional_email, u.full_name, u.password_hash, u.created_at,
              (SELECT COUNT(*) FROM mobile_posts WHERE user_id = u.id) as posts_count
              FROM mobile_users u WHERE u.institutional_email = :email LIMIT 0,1";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':email', $email);
    $stmt->execute();

    $num = $stmt->rowCount();

    if($num > 0){
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        // Verify Password
        if(password_verify($password, $row['password_hash'])){
            // Login Success
            $user_arr = array(
                "id" => $row['id'],
                "email" => $row['institutional_email'],
                "full_name" => $row['full_name'],
                "created_at" => $row['created_at'],
                "posts_count" => (int)$row['posts_count']
            );

            http_response_code(200);
            echo json_encode(array(
                "success" => true,
                "message" => "Login successful.",
                "user" => $user_arr
            ));
        } else {
            // Wrong Password
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Wrong password"));
        }
    } else {
        // User not found
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Wrong email"));
    }
} else {
    // Bad request
    http_response_code(400);
    echo json_encode(array("success" => false, "message" => "Incomplete data."));
}
?>