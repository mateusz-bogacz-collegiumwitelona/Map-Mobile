package com.example.map

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.ui.viewmodels.AuthViewModel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DependencyInjectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun hiltResolvesAuthViewModelAndItsDependencies() {
        composeRule.waitForIdle()

        val viewModel = ViewModelProvider(composeRule.activity)
            .get(AuthViewModel::class.java)

        assertNotNull(viewModel)
    }
}