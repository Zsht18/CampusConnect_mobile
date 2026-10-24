<?php
declare(strict_types=1);

final class MobileAiModeration
{
    public function __construct(private PDO $db)
    {
    }

    public function inspect(string $text): array
    {
        if (!$this->enabled()) {
            return ['available' => true, 'flagged' => false, 'categories' => []];
        }

        $containsProfanity = $this->containsProfanity($text);
        $apiKey = $this->apiKey();
        if ($apiKey === '') {
            return ['available' => false, 'flagged' => $containsProfanity, 'categories' => $containsProfanity ? ['profanity', 'moderation_unavailable'] : ['moderation_unavailable']];
        }

        $payload = json_encode([
            'model' => 'omni-moderation-latest',
            'input' => [['type' => 'text', 'text' => trim($text)]],
        ], JSON_UNESCAPED_SLASHES);
        if ($payload === false) {
            return ['available' => false, 'flagged' => $containsProfanity, 'categories' => $containsProfanity ? ['profanity', 'moderation_unavailable'] : ['moderation_unavailable']];
        }

        $response = $this->request($apiKey, $payload);
        if ($response === null) {
            return ['available' => false, 'flagged' => $containsProfanity, 'categories' => $containsProfanity ? ['profanity', 'moderation_unavailable'] : ['moderation_unavailable']];
        }

        $result = json_decode($response, true);
        $moderation = is_array($result) ? ($result['results'][0] ?? null) : null;
        if (!is_array($moderation) || !is_array($moderation['categories'] ?? null)) {
            return ['available' => false, 'flagged' => $containsProfanity, 'categories' => $containsProfanity ? ['profanity', 'moderation_unavailable'] : ['moderation_unavailable']];
        }

        $categories = array_keys(array_filter(
            $moderation['categories'],
            static fn($value): bool => $value === true
        ));
        if ($containsProfanity && !in_array('profanity', $categories, true)) {
            $categories[] = 'profanity';
        }
        return [
            'available' => true,
            'flagged' => (bool)($moderation['flagged'] ?? false) || $containsProfanity,
            'categories' => $categories,
        ];
    }

    public function recordPost(int $postId, array $review): void
    {
        if (!$review['flagged'] && $review['available']) {
            return;
        }
        $statement = $this->db->prepare(
            'INSERT INTO admin_mobile_post_moderation
                (mobile_post_id, status, ai_flagged, ai_categories)
             VALUES (?, 'pending', ?, ?)
             ON DUPLICATE KEY UPDATE
                status = 'pending', ai_flagged = VALUES(ai_flagged),
                ai_categories = VALUES(ai_categories)'
        );
        $statement->execute([
            $postId,
            $review['flagged'] ? 1 : 0,
            implode(', ', $review['categories']),
        ]);
    }

    public function recordComment(int $commentId, array $review): void
    {
        if (!$review['flagged'] && $review['available']) {
            return;
        }
        $statement = $this->db->prepare(
            'INSERT INTO admin_mobile_comment_moderation
                (mobile_comment_id, status, ai_flagged, ai_categories)
             VALUES (?, 'hidden', ?, ?)
             ON DUPLICATE KEY UPDATE
                status = 'hidden', ai_flagged = VALUES(ai_flagged),
                ai_categories = VALUES(ai_categories)'
        );
        $statement->execute([
            $commentId,
            $review['flagged'] ? 1 : 0,
            implode(', ', $review['categories']),
        ]);
    }

    private function enabled(): bool
    {
        try {
            $value = $this->db->query(
                'SELECT enabled FROM admin_ai_moderation_settings WHERE id = 1'
            )->fetchColumn();
            return $value !== false && (bool)$value;
        } catch (PDOException) {
            return false;
        }
    }

    private function request(string $apiKey, string $payload): ?string
    {
        if (function_exists('curl_init')) {
            $curl = curl_init('https://api.openai.com/v1/moderations');
            curl_setopt_array($curl, [
                CURLOPT_POST => true,
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_HTTPHEADER => [
                    'Authorization: Bearer ' . $apiKey,
                    'Content-Type: application/json',
                ],
                CURLOPT_POSTFIELDS => $payload,
                CURLOPT_TIMEOUT => 15,
            ]);
            $response = curl_exec($curl);
            $status = (int)curl_getinfo($curl, CURLINFO_HTTP_CODE);
            curl_close($curl);
            return is_string($response) && $status >= 200 && $status < 300 ? $response : null;
        }

        $context = stream_context_create([
            'http' => [
                'method' => 'POST',
                'header' => "Authorization: Bearer {$apiKey}\r\nContent-Type: application/json\r\n",
                'content' => $payload,
                'timeout' => 15,
                'ignore_errors' => true,
            ],
        ]);
        $response = @file_get_contents('https://api.openai.com/v1/moderations', false, $context);
        return is_string($response) ? $response : null;
    }

    private function apiKey(): string
    {
        $key = getenv('OPENAI_API_KEY');
        if (is_string($key) && trim($key) !== '') {
            return trim($key);
        }
        $key = $_SERVER['OPENAI_API_KEY'] ?? '';
        if (is_string($key) && trim($key) !== '') {
            return trim($key);
        }
        $localFiles = [
            __DIR__ . '/../CampusConnect_admin/CampusConnect_admin/config/openai.local.php',
            __DIR__ . '/config/openai.local.php',
        ];
        foreach ($localFiles as $file) {
            if (is_file($file)) {
                $value = require $file;
                if (is_string($value) && trim($value) !== '') {
                    return trim($value);
                }
            }
        }
        return '';
    }

    private function containsProfanity(string $text): bool
    {
        $normalized = strtolower($text);
        $normalized = strtr($normalized, [
            '0' => 'o',
            '1' => 'i',
            '3' => 'e',
            '4' => 'a',
            '5' => 's',
            '7' => 't',
            '@' => 'a',
            '$' => 's',
        ]);

        if (preg_match(
            '/\b(fuck|fucking|fucker|motherfucker|shit|bullshit|bitch|bastard|dick|cunt|slut|whore)\b/i',
            $normalized
        ) === 1) {
            return true;
        }

        // Catch common attempts to evade filtering with punctuation or spaces.
        $compact = preg_replace('/[^a-z]/', '', $normalized);
        if (!is_string($compact)) {
            return false;
        }

        foreach (['fuck', 'shit', 'bitch', 'bastard', 'dick', 'cunt', 'slut', 'whore'] as $term) {
            if (strpos($compact, $term) !== false) {
                return true;
            }
        }
        return false;
    }
}
