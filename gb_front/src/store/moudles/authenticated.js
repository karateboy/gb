/**
 * Created by user on 2017/1/12.
 */
import axios from 'axios';
import baseUrl from '../../baseUrl'

const state = {
    authenticated: false,
    user: {
        _id: "",
        password: "",
        name: "",
        phone: "",
        isAdmin: false,
        groupId: ""
    },
}

const getters = {
    isAuthenticated: state =>{
        return state.authenticated;
    },
    user: state =>{
        return state.user;
    }
}

const mutations = {
    updateAuthenticated: (state, payload) => {
        state.authenticated = payload.authenticated
        state.user = payload.user
        state.config = payload.config
    }
}

const actions = {
    logout : ({commit}) => {
        commit('updateAuthenticated', {authenticated:false, config:{}});
    }
}

export default {
    state,
    getters,
    mutations,
    actions
}
