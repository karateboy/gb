<template>
    <div class="sidebar-collapse">
        <ul class="nav metismenu" id="side-menu">
            <li class="nav-header">
                <div class="dropdown profile-element">
                    <a data-toggle="dropdown" class="dropdown-toggle" href="#">
                            <span class="clear"> <span class="block m-t-xs"> <strong
                                    class="font-bold">{{ user.name }}</strong>
                             </span> <span class="text-muted text-xs block">{{ groupInfoMap[user.groupId]}} <b
                                    class="caret"></b></span> </span> </a>
                    <ul class="dropdown-menu animated fadeInRight m-t-xs">
                        <li><a href="#">Logout</a></li>
                    </ul>
                </div>
                <div class="logo-element">
                    IN+
                </div>
            </li>
            <router-link tag="li" to="/" active-class="active" exact>
                <a><i class="fa fa-tachometer"
                      aria-hidden="true"></i> <span
                        class="nav-label">儀錶板</span></a></router-link>
            <li>
                <a><i class="fa fa-address-book-o" aria-hidden="true"></i><span class="nav-label">清運訂單管理</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li"
                                 :to="{name:'UnhandledOrder'}" active-class="active">
                        <a><i class="fa fa-telegram" aria-hidden="true"></i>
                            <span class="nav-label">未聯絡通報</span> </a></router-link>
                    <router-link tag="li" to="/Order/Mine" active-class="active"><a><i class="fa fa-th-large"></i> <span
                            class="nav-label">我的訂單</span> </a></router-link>
                    <router-link tag="li"
                                 :to="{name:'QueryOrder'}" active-class="active">
                        <a><i class="fa fa-search" aria-hidden="true"></i>
                            <span class="nav-label">訂單查詢</span> </a></router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-h-square" aria-hidden="true"></i><span class="nav-label">安養機構</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li"
                                 :to="{name:'QueryCareHouse'}" active-class="active">
                        <a><i class="fa fa-search" aria-hidden="true"></i>
                            <span class="nav-label">查詢安養機構</span> </a></router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-truck" aria-hidden="true"></i><span class="nav-label">車輛管理</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li"
                                 :to="{name:'AddVehicle'}" active-class="active">
                        <a><i class="fa fa-plus" aria-hidden="true"></i>
                            <span class="nav-label">新增車輛</span> </a></router-link>

                    <router-link tag="li"
                                 :to="{name:'DelVehicle'}" active-class="active">
                        <a><i class="fa fa-trash" aria-hidden="true"></i>
                            <span class="nav-label">刪除車輛</span> </a></router-link>

                    <router-link tag="li"
                                 :to="{name:'UpdateVehicle'}" active-class="active">
                        <a><i class="fa fa-pencil" aria-hidden="true"></i>
                            <span class="nav-label">更新車輛資料</span> </a></router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-cog" aria-hidden="true"></i><span class="nav-label">系統管理</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li"
                                 :to="{name:'AddUser'}" active-class="active">
                        <a><i class="fa fa-plus" aria-hidden="true"></i>
                            <span class="nav-label">新增使用者</span> </a></router-link>

                    <router-link tag="li"
                                 :to="{name:'DelUser'}" active-class="active">
                        <a><i class="fa fa-trash" aria-hidden="true"></i>
                            <span class="nav-label">刪除使用者</span> </a></router-link>

                    <router-link tag="li"
                                 :to="{name:'UpdateUser'}" active-class="active">
                        <a><i class="fa fa-pencil" aria-hidden="true"></i>
                            <span class="nav-label">更新使用者</span> </a></router-link>
                </ul>
            </li>
        </ul>
    </div>
</template>
<style scoped>

</style>
<script>
    import {mapGetters} from 'vuex'
    import axios from 'axios'
    export default{
        data(){
            axios.get('/Group').then((resp) => {
                const ret = resp.data
                for (let groupInfo of ret) {
                    this.groupInfoMap[groupInfo.id] = groupInfo.name
                }
            })
            return {
                groupInfoMap: {}
            }
        },
        computed: {
            ...mapGetters(['user'])
        }
    }


</script>
