package su.afk.yummy.tv.domain.account.model

/** Пользователь закрыл свои списки аниме настройкой приватности — сервер отказывает в доступе. */
class UserAnimeListsHiddenException : RuntimeException("User anime lists are hidden")
