# 用户头像上传

**Redis / Kafka**：未使用。  
**MySQL**：`user.avatar_url`。  
**本地文件**：头像二进制落盘，通过静态 URL 访问。

## POST /api/v1/users/me/avatar

### 前端

- `frontend/src/api/userProfile.ts` → `uploadUserAvatar(file)` → `FormData` + `POST /users/me/avatar`。

### 后端

| 类 | 方法 |
|-----|------|
| `UserMeAvatarController` | `uploadAvatar(userId, file, request)` |
| `UserAvatarStorageService` | `saveAndBuildUrl(file, request)` → 保存文件，拼出对外 URL |
| `UserProfileServiceImpl` | `updateAvatarUrl(userId, url)` → `UserMapper.selectById` + `updateById` |

### 读取文件

- `UserAvatarFileController`：`GET /api/v1/files/user-avatars/{filename}`（**无 JWT**）。

---

## Mermaid

```mermaid
sequenceDiagram
  participant V as ProfileView.vue
  participant UP as userProfile.ts
  participant X as axios
  participant F as JwtAuthenticationFilter
  participant C as UserMeAvatarController
  participant ST as UserAvatarStorageService
  participant S as UserProfileServiceImpl
  participant M as UserMapper
  participant Disk as 本地磁盘
  participant DB as MySQL user

  V->>UP: uploadUserAvatar(file)
  UP->>X: POST /users/me/avatar multipart
  F->>C: uploadAvatar
  C->>ST: saveAndBuildUrl
  ST->>Disk: write
  ST-->>C: publicUrl
  C->>S: updateAvatarUrl(userId,url)
  S->>M: updateById
  M->>DB: UPDATE user
```
