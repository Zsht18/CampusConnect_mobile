<?php
// upload_profile_pic.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$base64_image = isset($_POST['image']) ? $_POST['image'] : '';

if($user_id > 0 && !empty($base64_image)){
    $upload_dir = 'uploads/';
    if(!file_exists($upload_dir)){
        mkdir($upload_dir, 0755, true);
    }

    $image_data = base64_decode($base64_image);
    $filename = 'profile_' . $user_id . '_' . time() . '.jpg';
    $file_path = $upload_dir . $filename;

    if(file_put_contents($file_path, $image_data)){
        $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
        $server_host = $_SERVER['HTTP_HOST'];
        $script_path = dirname($_SERVER['PHP_SELF']);
        $profile_pic_url = $protocol . "://" . $server_host . $script_path . "/" . $file_path;

        $query = "UPDATE mobile_users SET profile_pic_url = :profile_pic_url WHERE id = :user_id";
        $stmt = $conn->prepare($query);
        $stmt->bindParam(':profile_pic_url', $profile_pic_url);
        $stmt->bindParam(':user_id', $user_id);

        if($stmt->execute()){
            http_response_code(200);
            echo json_encode(array(
                "success" => true,
                "message" => "Profile picture uploaded successfully.",
                "profile_pic_url" => $profile_pic_url
            ));
        } else {
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Failed to update database."));
        }
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Failed to save image file."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Incomplete data."));
}
?>