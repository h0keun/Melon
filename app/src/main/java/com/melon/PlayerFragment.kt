package com.melon

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

class PlayerFragment: Fragment(R.layout.fragment_player) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    companion object { // newInstance 로 인자를 넘겨줄 때 apply 를 통해 arguments 를 쉽게 추가해줄 수 있다.
        fun newInstance(): PlayerFragment {
            return PlayerFragment()
        }
    }

}