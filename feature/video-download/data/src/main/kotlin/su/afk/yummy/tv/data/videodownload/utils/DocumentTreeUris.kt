package su.afk.yummy.tv.data.videodownload.utils

import android.net.Uri
import android.provider.DocumentsContract

/**
 * SAF отдаёт tree-URI (`content://…/tree/<id>`), по которому нельзя ни читать метаданные,
 * ни создавать документы — сначала его нужно превратить в document-URI самой папки.
 */
internal fun Uri.treeDocumentUri(): Uri =
    DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))
