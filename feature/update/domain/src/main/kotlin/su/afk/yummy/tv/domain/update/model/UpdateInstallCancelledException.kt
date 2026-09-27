package su.afk.yummy.tv.domain.update.model

/**
 * Пользователь отменил установку обновления в системном диалоге подтверждения
 * (`PackageInstaller.STATUS_FAILURE_ABORTED`). Это не сбой приложения, а ожидаемый исход,
 * поэтому такой случай попадает в аналитику как обычное событие `update_error`,
 * но НЕ репортится как non-fatal ошибка в AppMetrica.
 */
class UpdateInstallCancelledException(message: String) : IllegalStateException(message)
