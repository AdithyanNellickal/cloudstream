package com.lagradost.cloudstream3.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.lagradost.cloudstream3.AcraApplication.Companion.openBrowser
import com.lagradost.cloudstream3.MainActivity.Companion.afterRepositoryLoadedEvent
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.RepositoryManager.PREBUILT_REPOSITORIES
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.isTvSettings
import com.lagradost.cloudstream3.ui.settings.extensions.PUBLIC_REPOSITORIES_LIST
import com.lagradost.cloudstream3.ui.settings.extensions.PluginsViewModel
import com.lagradost.cloudstream3.ui.settings.extensions.RepoAdapter
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.cloudstream3.utils.UIHelper.fixPaddingStatusbar
import kotlinx.android.synthetic.main.fragment_extensions.*
import kotlinx.android.synthetic.main.fragment_setup_media.*


class SetupFragmentExtensions : Fragment() {
    companion object {
        const val SETUP_EXTENSION_BUNDLE_IS_SETUP = "isSetup"

        /**
         * If false then this is treated a singular screen with a done button
         * */
        fun newInstance(isSetup: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(SETUP_EXTENSION_BUNDLE_IS_SETUP, isSetup)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup_extensions, container, false)
    }

    override fun onResume() {
        super.onResume()
        afterRepositoryLoadedEvent += ::setRepositories
    }

    override fun onStop() {
        super.onStop()
        afterRepositoryLoadedEvent -= ::setRepositories
    }

    private fun setRepositories(success: Boolean = true) {
        main {
            val repositories = RepositoryManager.getRepositories() + PREBUILT_REPOSITORIES
            val hasRepos = repositories.isNotEmpty()
            repo_recycler_view?.isVisible = hasRepos
            blank_repo_screen?.isVisible = !hasRepos

            if (hasRepos) {
                repo_recycler_view?.adapter = RepoAdapter(true, {}, {
                    PluginsViewModel.downloadAll(activity, it.url, null)
                }).apply { updateList(repositories) }
            } else {
                list_repositories?.setOnClickListener {
                    // Open webview on tv if browser fails
                    val isTv = it.context.isTvSettings()
                    openBrowser(PUBLIC_REPOSITORIES_LIST, isTv, this)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.fixPaddingStatusbar(setup_root)
        val isSetup = arguments?.getBoolean(SETUP_EXTENSION_BUNDLE_IS_SETUP) ?: false

        with(context) {
            if (this == null) return
            setRepositories()

            if (!isSetup) {
                next_btt.setText(R.string.setup_done)
            }
            prev_btt?.isVisible = isSetup

            next_btt?.setOnClickListener {
                // Continue setup
                if (isSetup)
                    findNavController().navigate(R.id.action_navigation_setup_extensions_to_navigation_setup_provider_languages)
                else
                    findNavController().navigate(R.id.navigation_home)
            }

            prev_btt?.setOnClickListener {
                findNavController().navigate(R.id.navigation_setup_language)
            }
        }
    }


}