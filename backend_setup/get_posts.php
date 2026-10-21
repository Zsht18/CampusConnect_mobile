<?php
// get_posts.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once 'db_connect.php';

$category = isset($_GET['category']) ? trim($_GET['category']) : '';
$user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
$search = isset($_GET['search']) ? trim($_GET['search']) : '';

$query = "SELECT p.id, p.user_id, p.title, p.category, p.content, p.file_url, p.is_anonymous, p.approvals, p.shares, p.created_at,
          IF(p.is_anonymous = 1, CONCAT(IFNULL(SUBSTRING(u.username, 1, 1), 'A'), '.....'), u.username) as author_name,
          u.username as owner_username,
          IF(p.is_anonymous = 1, NULL, u.profile_pic_url) as profile_pic_url,
          (SELECT COUNT(*) FROM mobile_comments WHERE post_id = p.id) as comments_count
          FROM mobile_posts p
          LEFT JOIN mobile_users u ON p.user_id = u.id ";

$conditions = array();
$params = array();

if($user_id > 0){
    $conditions[] = "p.user_id = :user_id";
    $params[':user_id'] = $user_id;
}

if(!empty($category) && $category !== 'All'){
    $conditions[] = "p.category = :category";
    $params[':category'] = $category;
}

if(!empty($search)){
    $conditions[] = "(p.title LIKE :search OR p.content LIKE :search OR p.category LIKE :search)";
    $params[':search'] = '%' . $search . '%';
}

if(count($conditions) > 0){
    $query .= " WHERE " . implode(" AND ", $conditions);
}

$query .= " ORDER BY p.created_at DESC";

$stmt = $conn->prepare($query);
foreach($params as $key => &$val){
    $stmt->bindParam($key, $val);
}

$stmt->execute();
$posts = $stmt->fetchAll(PDO::FETCH_ASSOC);

http_response_code(200);
echo json_encode($posts);
?>