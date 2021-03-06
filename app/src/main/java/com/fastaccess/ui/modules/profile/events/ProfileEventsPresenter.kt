package com.fastaccess.ui.modules.profile.events

import android.net.Uri
import android.view.View
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.fastaccess.data.dao.NameParser
import com.fastaccess.data.dao.Pageable
import com.fastaccess.data.dao.SimpleUrlsModel
import com.fastaccess.data.dao.model.Event
import com.fastaccess.data.dao.types.EventsType
import com.fastaccess.provider.rest.RestProvider
import com.fastaccess.provider.scheme.SchemeParser
import com.fastaccess.ui.base.mvp.presenter.BasePresenter
import com.fastaccess.ui.modules.repos.RepoPagerActivity
import com.fastaccess.ui.modules.repos.code.commit.details.CommitPagerActivity
import com.fastaccess.ui.modules.repos.code.releases.ReleasesListActivity
import java.util.*

/**
 * Created by Kosh on 11 Nov 2016, 12:36 PM
 */

class ProfileEventsPresenter : BasePresenter<ProfileEventsMvp.View>(), ProfileEventsMvp.Presenter {
    private val eventsModels = ArrayList<Event>()
    private var page: Int = 0
    private var previousTotal: Int = 0
    private var lastPage = Integer.MAX_VALUE

    override fun getCurrentPage(): Int  = page

    override fun getPreviousTotal(): Int = previousTotal

    override fun setCurrentPage(page: Int) {
        this.page = page
    }

    override fun setPreviousTotal(previousTotal: Int) {
        this.previousTotal = previousTotal
    }

    override fun onCallApi(page: Int, parameter: String?) {
        if (page == 1 || parameter.isNullOrEmpty()) {
            lastPage = Integer.MAX_VALUE
            sendToView { view -> view.getLoadMore().reset() }
        }
        if (page > lastPage || lastPage == 0) {
            sendToView({ it.hideProgress() })
            return
        }
        currentPage = page
        makeRestCall<Pageable<Event>>(RestProvider.getUserService().getUserEvents(parameter!!, page)) { response ->
            lastPage = response.last
            sendToView { view -> view.onNotifyAdapter(response.items, page) }
        }
    }

    override fun onSubscribed() {
        sendToView { view -> view.showProgress(0) }
    }

    override fun onError(throwable: Throwable) {
        onWorkOffline()
        super.onError(throwable)
    }

    override fun getEvents(): ArrayList<Event> {
        return eventsModels
    }

    override fun onWorkOffline() {
        //TODO
        sendToView({ it.hideProgress() })
    }

    override fun onItemClick(position: Int, v: View, item: Event) {
        if (item.type == EventsType.ForkEvent) {
            val parser = NameParser(item.payload.forkee.htmlUrl)
            RepoPagerActivity.startRepoPager(v.context, parser)
        } else {
            val payloadModel = item.payload
            payloadModel?.let {
                if (it.head != null) {
                    if (it.commits != null && it.commits.size > 1) {
                        sendToView { view -> view.onOpenCommitChooser(it.commits) }
                    } else {
                        val repoModel = item.repo
                        val nameParser = NameParser(repoModel.url)
                        val intent = CommitPagerActivity.createIntent(v.context, nameParser.name,
                                nameParser.username, it.head, true)
                        v.context.startActivity(intent)
                    }
                } else if (it.issue != null) {
                    SchemeParser.launchUri(v.context, Uri.parse(it.issue.htmlUrl), true)
                } else if (it.pullRequest != null) {
                    SchemeParser.launchUri(v.context, Uri.parse(it.pullRequest.htmlUrl), true)
                } else if (it.comment != null) {
                    SchemeParser.launchUri(v.context, Uri.parse(it.comment.htmlUrl), true)
                } else if (item.type == EventsType.ReleaseEvent && it.release != null) {
                    val nameParser = NameParser(it.release.htmlUrl)
                    v.context.startActivity(ReleasesListActivity.getIntent(v.context, nameParser.username, nameParser.name,
                            it.release.id))

                } else if (item.type == EventsType.CreateEvent && "tag".equals(it.refType, ignoreCase = true)) {
                    val repoModel = item.repo
                    val nameParser = NameParser(repoModel.url)
                    v.context.startActivity(ReleasesListActivity.getIntent(v.context, nameParser.username, nameParser.name,
                            it.ref))
                } else {
                    val repoModel = item.repo
                    if (item.repo != null) SchemeParser.launchUri(v.context, Uri.parse(repoModel.name), true)
                }
            }
        }
    }

    override fun onItemLongClick(position: Int, v: View, item: Event) {
        if (item.type == EventsType.ForkEvent) {
            if (view != null) {
                view!!.onOpenRepoChooser(Stream.of(SimpleUrlsModel(item.repo.name, item.repo.url),
                        SimpleUrlsModel(item.payload.forkee.fullName, item.payload.forkee.htmlUrl))
                        .collect(Collectors.toCollection(::arrayListOf)))
            }
        } else {
            onItemClick(position, v, item)
        }
    }
}
