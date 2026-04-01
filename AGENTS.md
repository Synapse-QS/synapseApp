# Synapse Social — Engineering Standards

All contributors and AI agents must follow these standards without exception.

---

## Architecture

Synapse Social uses **Kotlin Multiplatform (KMP)** with **Clean Architecture + MVVM**.

```
Presentation  →  ViewModel  →  Domain (UseCases / Interfaces)  →  Data (Repos / DTOs / Mappers)
```

- Domain has zero knowledge of any framework, SDK, or platform.
- Data owns all external concerns: network, storage, SDKs.
- Presentation is dumb — it renders state and forwards events only.

---

## Shared Module (`:shared`)

**Domain**
- One `operator fun invoke()` per UseCase. No constructor logic.
- Repositories are interfaces only.
- Models are pure Kotlin data classes. No Room/SQL annotations.

**Data**
- Repository impls orchestrate DataSources.
- DTOs mirror the external schema exactly.
- Mappers are mandatory. DTOs never reach Domain or UI.

**DI:** Koin. Exposed to iOS via `DependencyContainer`.

---

## Android (`:app`)

- 100% Jetpack Compose. No XML.
- One ViewModel per screen, holding `StateFlow<UiState>`.
- DI via Hilt.
- Use `MaterialTheme.colorScheme`, `Spacing`, and `stringResource()`. No hardcoded values.

---

## iOS (`:iosApp`)

- 100% SwiftUI.
- ViewModels use `ObservableObject`, `@Published`, `@MainActor`.
- Consume UseCases directly from the shared framework.
- Use `IosSecureStorage` for Keychain operations.

---

## Code Standards

- UseCases always return `Result<T>` or a sealed `Either` — never raw exceptions.
- No `android.*` or `java.*` imports in `shared/commonMain`.
- Business logic lives in UseCases, not ViewModels or Views.

**Naming**

| Type | Convention |
| --- | --- |
| UseCase | `SendMessageUseCase` |
| Repository | `ChatRepository` (interface), `SupabaseChatRepository` (impl) |
| ViewModel | `ChatViewModel` |
| DTO | `UserDto` |
| UI State | `ChatUiState` |
| Mapper | `UserMapper.toDomain()` |

---

## Pre-Commit Checklist

- [ ] `./gradlew build` passes
- [ ] No framework imports in `shared/commonMain`
- [ ] No hardcoded strings, colors, or dimensions in `:app`
- [ ] Business logic is in a UseCase
- [ ] Every DTO has a mapper to a Domain Model
- [ ] Diff self-reviewed for dead code and TODOs

---

## Pull Requests

- Title format: `✨ feat: Short description` or `🐛 fix: Short description`
- Use `.github/PULL_REQUEST_TEMPLATE/feature.md` or `bug_fix.md`
- Explain *why*, not just *what*

---

## Key Paths

| What | Path |
| --- | --- |
| Shared Domain | `shared/src/commonMain/kotlin/.../domain/` |
| Shared Data | `shared/src/commonMain/kotlin/.../data/` |
| Android UI | `app/src/main/java/.../` |
| iOS UI | `iosApp/iosApp/` |
| PR Templates | `.github/PULL_REQUEST_TEMPLATE/` |
