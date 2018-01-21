/**
 * Created by user on 2017/1/12.
 */

const state = {
    authenticated: false,
    user: {
        _id: "",
        password: "",
        name: "",
        phone: "",
        groupId: ""
    },
}

const getters = {
    isAuthenticated: state => {
        return state.authenticated;
    },
    user: state => {
        return state.user;
    },
    isAdmin: state => {
        return state.user.groupId == "Admin";
    },
    isSales: state => {
        return state.user.groupId == "Sales";
    },
    isIntern: state => {
        return state.user.groupId == "Intern";
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
    logout: ({
        commit
    }) => {
        commit('updateAuthenticated', {
            authenticated: false,
            config: {}
        });
    }
}

export default {
    state,
    getters,
    mutations,
    actions
}